"""
1.8.0 Guhdromenland, island "herinnering" (dromen-ontwerp/BOUWPLAN_180.md part E; design ontwerp-guhs180-dromen.md, section
"Playtest testbuild 2": the revamp, short and sweet). Java: feature/herinnering (HerinneringFeature, HerinneringBellen,
HerinneringScenes).

  build    the island template data/guhs/structure/dromen/eiland_herinnering.nbt: a small round island with three little
           memories of Snoes round the steiger (his first kaasknabbel in the grass, his first night with a Blub in a nest
           under its own little night sky, his first stargazing on a small hill), each with a herinneringsbelletje (a
           hollow zeepbelglas ball, see BELLEN: the same numbers are in HerinneringBellen.java) and a little memory-Snoes;
           its island json data/guhs/dromen/eiland/herinnering.json (version 2) and the texts of the island and its three
           retro films (Dutch in nl_nl, English in tools/lang/en/c212_herinnering.json). Version 1 (the Guh-familiedag)
           texts are removed from the lang files.

Standalone (from the project root): PYTHONPATH=tools python tools/features/herinnering.py
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, "tools")
import make_structures as ms  # noqa: E402

X, Y, Z = 76, 40, 76
C, T = 38, 22                      # the island's centre spot (template block) and the height of its top layer
VERSIE = 2                         # 2 = the revamp: three little memories of Snoes (no more familiedag)
TAG = "guhs_herinnering"           # the little memory-Snoeses: clicking them gives a memory line (HerinneringFeature)

# the three herinneringsbelletjes: id -> (centre x, y, z in the template, radius, the little "film still" inside)
# KEEP IN SYNC with HerinneringBellen.BELLEN (relative there: minus (C, T, C))
BELLEN = {
    "knabbel": ((22, T + 4, 36), 2.4, "guhs:lobby_gouden_knabbel"),
    "blub": ((47, T + 4, 48), 2.4, "guhs:guhnestje"),
    "sterren": ((26, T + 4, 33), 2.4, "guhs:sterrenlantaarn"),
}
# the three little scenes (the films play there): (x, z) on the island's top
KNABBEL = (27, 40)
NEST = (40, 52)
HEUVEL = (31, 27)
# the five films of version 1 (the Guh-familiedag): their texts are removed from the lang files on a run
OUD_SCENES = ("quiz", "prik", "koeken", "oma", "snoes")


def _parse(s):
    """'guhs:x[a=b,c=d]' -> ('guhs:x', {'a': 'b', 'c': 'd'}); no namespace = guhs."""
    naam, props = s, {}
    if "[" in s:
        naam, rest = s.split("[", 1)
        for kv in rest.rstrip("]").split(","):
            if kv:
                k, v = kv.split("=")
                props[k] = v
    if ":" not in naam:
        naam = "guhs:" + naam
    return naam, props


class Eiland(ms.Structure):
    """The few Scene calls of the render (dromen-ontwerp vibekit/droomkit) on top of a structure."""

    def __init__(self):
        super().__init__((X, Y, Z))
        self.namen = set()

    def zet(self, x, y, z, s, keep=False):
        x, y, z = int(x), int(y), int(z)
        if not self.inside(x, y, z) or (keep and (x, y, z) in self.blocks):
            return
        naam, props = _parse(s)
        if naam == "minecraft:air":
            self.blocks.pop((x, y, z), None)
            return
        self.namen.add(naam)
        self.set(x, y, z, naam, props)

    def is_air(self, x, y, z):
        return (int(x), int(y), int(z)) not in self.blocks

    def naam_op(self, x, y, z):
        b = self.blocks.get((int(x), int(y), int(z)))
        return b[0] if b else None

    def vul(self, x1, y1, z1, x2, y2, z2, s, keep=False):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.zet(x, y, z, s, keep)

    def wanden(self, x1, y1, z1, x2, y2, z2, s):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                if x in (x1, x2) or z in (z1, z2):
                    for y in range(min(y1, y2), max(y1, y2) + 1):
                        self.zet(x, y, z, s)

    def lucht(self, x1, y1, z1, x2, y2, z2):
        self.vul(x1, y1, z1, x2, y2, z2, "minecraft:air")

    def bol(self, cx, cy, cz, rx, ry, rz, s, hollow=0.0, keep=False):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            for y in range(int(cy - ry - 1), int(cy + ry + 2)):
                for z in range(int(cz - rz - 1), int(cz + rz + 2)):
                    if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2 > 1:
                        continue
                    if hollow and ((x - cx) / (rx - hollow)) ** 2 + ((y - cy) / (ry - hollow)) ** 2 + ((z - cz) / (rz - hollow)) ** 2 <= 1:
                        continue
                    self.zet(x, y, z, s, keep)

    def cilinder(self, cx, cz, r, y1, y2, s, hollow=0.0):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            for z in range(int(cz - r - 1), int(cz + r + 2)):
                d = math.hypot(x - cx, z - cz)
                if d <= r and not (hollow and d <= r - hollow):
                    for y in range(y1, y2 + 1):
                        self.zet(x, y, z, s)

    def boom(self, x, y, z, h=4, r=2):
        for yy in range(y, y + h):
            self.zet(x, yy, z, "guhs:pluizenboom_stam[axis=y]")
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                for dy in range(-1, 2):
                    if abs(dx) + abs(dz) + abs(dy) * 1.5 <= r + 0.6 and (dx, dz) != (0, 0):
                        self.zet(x + dx, y + h + dy - 1, z + dz, "guhs:pluizenboom_bladeren", keep=True)
        self.zet(x, y + h, z, "guhs:pluizenboom_bladeren")
        self.zet(x, y + h + 1, z, "guhs:pluizenboom_bladeren", keep=True)

    def npc(self, x, y, z, render_yaw, kind, herinnering=True):
        """A person of the memory (render yaw 0 = looking north, as in the renders; Minecraft yaw 0 = south)."""
        nbt = {"id": "guhs:guh_npc", "Kind": kind, "PersistenceRequired": ms.Byte(1),
               "Rotation": ms.floats(float((render_yaw + 180) % 360), 0.0)}
        if herinnering:
            nbt["Tags"] = ms.NbtList(8, [TAG])
        self.entity(x, y, z, nbt)


def bouw():
    S = Eiland()
    rng = random.Random(216)

    # ------------------------------------------------------------------------------------------- the floating island
    r, diepte, fase = 21, 15, [rng.random() * 2 * math.pi for _ in range(4)]

    def rand(a):
        return r * (1 + 0.09 * (0.6 * math.sin(3 * a + fase[0]) + 0.4 * math.sin(5 * a + fase[1]) + 0.25 * math.sin(8 * a + fase[2])))

    def binnen(x, z):
        return math.hypot(x - C, z - C) <= rand(math.atan2(z - C, x - C))

    for x in range(X):
        for z in range(Z):
            R0 = rand(math.atan2(z - C, x - C))
            d = math.hypot(x - C, z - C) / R0
            if d > 1:
                continue
            h = int(diepte * (1 - d) ** 0.7 + rng.random() * 1.5) + 1
            for k in range(h + 1):                                    # a closed top layer everywhere: nowhere to fall through
                b = "guhs:knuffelgras" if k == 0 else "guhs:pluiswolblok" if k <= 2 else \
                    "guhs:droomzandblok" if k >= h - 1 and d < 0.3 else "guhs:knuffelsteen"
                S.zet(x, T - k, z, b)
            if d > 0.82 and rng.random() < 0.12:
                for k in range(1, rng.randint(2, 4)):
                    S.zet(x, T - h - k, z, "guhs:wolkenweide_rank", keep=True)

    def belletje(naam):
        (x, y, z), rr, binnenin = BELLEN[naam]
        S.bol(x, y, z, rr, rr, rr, "guhs:zeepbelglas", hollow=1)
        S.zet(x, y, z, binnenin)

    # ------------------------------------------------------------------- little paths of mokka plaster (in the top layer)
    def pad(x1, z1, x2, z2):
        n = int(max(abs(x2 - x1), abs(z2 - z1)) * 2) + 1
        for i in range(n + 1):
            t = i / n
            xx, zz = int(round(x1 + (x2 - x1) * t)), int(round(z1 + (z2 - z1) * t))
            if binnen(xx, zz):
                S.zet(xx, T, zz, "guhs:pleister_mokka" if (xx + zz) % 3 else "guhs:tegel_pootjes")

    dx, dz = math.sin(math.radians(36)), -math.cos(math.radians(36))
    aankomst = (int(round(C + dx * 14)), int(round(C + dz * 14)))
    pad(aankomst[0], aankomst[1], C, C)
    for (px, pz) in (KNABBEL, NEST, HEUVEL):
        pad(C, C, px, pz)

    # ---------------------------------------------------- 1. de eerste kaasknabbel: a little meadow with one golden knabbel
    kx, kz = KNABBEL
    for (x, z) in ((kx - 2, kz - 1), (kx - 1, kz + 2), (kx + 1, kz - 2), (kx - 3, kz + 1), (kx + 2, kz + 2), (kx - 2, kz - 3)):
        S.zet(x, T + 1, z, "guhs:pluisgras" if (x + z) % 2 else "guhs:roze_guhbloem", keep=True)
    S.zet(kx, T + 1, kz, "guhs:lobby_gouden_knabbel")
    S.npc(kx + 1.5, T + 1, kz + .5, 270, "snoes")                        # little Snoes, sniffing at it (looking west)
    S.zet(kx + 2, T + 1, kz - 2, "guhs:dierentuinbordje[facing=east]")
    belletje("knabbel")

    # ------------------------------------------- 2. het eerste nachtje met een Blub: a nest under a little night sky
    nx, nz = NEST
    for x in range(nx - 4, nx + 5):
        for z in range(nz - 4, nz + 5):
            dd = math.hypot(x - nx, z - nz)
            if dd > 4.3:
                continue
            S.zet(x, T + 5, z, "guhs:sterrenplafond")                    # its own night sky
            if dd > 3.3 and not (z < nz - 2 and abs(x - nx) <= 1):       # round wall, open to the north (the path)
                S.vul(x, T + 1, z, x, T + 4, z, "guhs:sterrenplafond")
    S.zet(nx, T + 1, nz, "guhs:guhnestje")
    S.zet(nx + 2, T + 1, nz + 1, "guhs:maanlampje_maan[facing=north,lit=true]")      # its own little light
    S.zet(nx - 2, T + 1, nz + 1, "guhs:maanbloemetje[open=true]")
    S.npc(nx + .5, T + 1.45, nz + .5, 0, "snoes")                        # little Snoes on the nest
    S.entity(nx - 1 + .5, T + 1, nz + .5, {"id": "guhs:blubguh", "BlubSoort": "blauw", "Meter": 30, "Tint": 0,
                                         "Grootte": ms.Float(1.0), "NoAI": ms.Byte(1), "PersistenceRequired": ms.Byte(1),
                                         "Rotation": ms.floats(270.0, 0.0)})          # ...and the Blub snuggled up to him
    belletje("blub")

    # ------------------------------------------------- 3. het eerste sterrenkijken: a little hill, every step one block
    hx, hz = HEUVEL
    for (rr, hoogte) in ((5.2, 1), (3.6, 2), (2.2, 3)):
        for x in range(hx - 6, hx + 7):
            for z in range(hz - 6, hz + 7):
                if math.hypot(x - hx, z - hz) <= rr and binnen(x, z):
                    for y in range(T + 1, T + hoogte):
                        S.zet(x, y, z, "guhs:pluiswolblok")
                    S.zet(x, T + hoogte, z, "guhs:knuffelgras")
    S.zet(hx, T + 4, hz - 1, "guhs:guh_telescoop[facing=north]")
    S.npc(hx + .5, T + 4, hz + 1.5, 180, "snoes")                        # little Snoes, looking up (north)
    for (x, z) in ((hx - 4, hz + 2), (hx + 4, hz - 1), (hx - 1, hz - 4)):
        S.zet(x, T + 2, z, "guhs:maanbloemetje[open=true]", keep=True)
    belletje("sterren")

    # ---------------------------------------------------------------- the steiger to the plein (36 degrees, north-east)
    px, pz = -dz, dx
    for k in [i / 2 for i in range(30, 70)]:
        for w in (-1, 0, 1):
            x, z = int(round(C + dx * k + px * w)), int(round(C + dz * k + pz * w))
            if 0 <= x < X and 0 <= z < Z and not binnen(x, z):
                S.zet(x, T, z, "guhs:wolkenblok_wit_plaat[type=top,waterlogged=false]" if w == 0
                      else "guhs:wolkenblok_roze_plaat[type=top,waterlogged=false]", keep=True)
    for k in (12, 18):                                                     # lanterns along the way in
        x, z = int(round(C + dx * k + px * 2)), int(round(C + dz * k + pz * 2))
        S.zet(x, T + 1, z, "guhs:lampion_roze[hanging=false]" if k == 12 else "guhs:lampion_geel[hanging=false]")

    # ------------------------------------------------------ welcome sign, a low white fence (open to the steiger), flowers
    S.zet(aankomst[0] - 2, T + 1, aankomst[1] + 1, "guhs:dierentuinbordje[facing=north]")
    for x in range(X):
        for z in range(Z):
            if binnen(x, z) and not all(binnen(x + a, z + b) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                naar_steiger = (x - C) * dx + (z - C) * dz > 0 and abs((x - C) * dz - (z - C) * dx) < 5
                if S.is_air(x, T + 1, z) and not naar_steiger:
                    S.zet(x, T + 1, z, "guhs:guh_hekje_wit")
    bloemen = ("guhs:roze_guhbloem", "guhs:pluisgras", "guhs:maanbloemetje[open=true]", "guhs:pluisgras")
    vrij = set()
    for (bx, by, bz), rr, _ in BELLEN.values():
        for a in range(-3, 4):
            for b in range(-3, 4):
                vrij.add((bx + a, bz + b))
    for x in range(X):
        for z in range(Z):
            if binnen(x, z) and S.is_air(x, T + 1, z) and S.naam_op(x, T, z) == "guhs:knuffelgras" \
                    and (x, z) not in vrij and rng.random() < 0.06:
                S.zet(x, T + 1, z, rng.choice(bloemen))
    for (x, z) in ((48, 30), (26, 50), (50, 40), (36, 22)):
        if binnen(x, z) and S.is_air(x, T + 1, z):
            S.boom(x, T + 1, z, h=5, r=2)
    return S


def _bestaat(naam):
    ns, pad = naam.split(":")
    if ns == "minecraft":
        return True
    return os.path.exists(os.path.join("src", "main", "resources", "assets", ns, "blockstates", pad + ".json"))


def _steiger(k):
    dx, dz = math.sin(math.radians(36)), -math.cos(math.radians(36))
    return round(dx * k, 2) + 0.5, round(dz * k, 2) + 0.5


def build():
    S = bouw()
    mist = sorted(n for n in S.namen if not _bestaat(n))
    if mist:
        raise SystemExit(f"herinnering: unknown blocks {mist}")
    S.save("dromen/eiland_herinnering")
    sx, sz = _steiger(30)
    ax, az = _steiger(14)
    yaw = math.degrees(math.atan2(ax, -az))       # look back to the middle of the island (Minecraft yaw: 0 = +z)
    opzet = {"versie": VERSIE, "midden": [C, T, C], "aankomst": {"plek": [ax, 1, az], "yaw": round(yaw, 1)},
             "steiger": [sx, 1, sz]}
    pad = os.path.join("src", "main", "resources", "data", "guhs", "dromen", "eiland", "herinnering.json")
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(opzet, f, indent=2)
        f.write("\n")
    return len(S.blocks), len(S.entities)


# ---------------------------------------------------------------------------------------------------------- texts
SCENE = "scene.guhs.herinnering_"
SCENE = "scene.guhs.herinnering_"
# keys of version 1 that are gone (besides the scene texts of OUD_SCENES)
OUD_KEYS = ("gui.guhs.herinnering.persoon",)
TEKSTEN = {  # key -> (Dutch, English)
    "gui.guhs.herinnering.bel": ("Een herinneringsbelletje... er zit een filmpje in.", "A memory bubble... there's a little film inside."),
    "gui.guhs.herinnering.bel_gezien": ("Dit filmpje ken je al. Klik op het belletje om het nog eens te zien.",
                                        "You know this film already. Click the bubble to watch it again."),
    "gui.guhs.herinnering.bel_dicht": ("Dit belletje is nog dicht. Kijk eerst de andere twee herinneringen.",
                                       "This bubble is still shut. Watch the other two memories first."),
    "gui.guhs.herinnering.persoon_snoes": ("Een kleine Snoes, van lang geleden. Hij ziet jou niet. Hij is in zijn herinnering.",
                                           "A little Snoes, from long ago. He doesn't see you. He's inside his memory."),
    "gui.guhs.herinnering.stukje": ("Stukje gevonden: de herinnering. Snoes dacht dat hij altijd alleen was... maar dat was hij nooit helemaal.",
                                    "Piece found: the memory. Snoes thought he was always alone... but he never quite was."),
    "gui.guhs.herinnering.voortgang": ("Herinneringen gezien: %s van 3", "Memories seen: %s of 3"),
    # 1. the first kaasknabbel
    SCENE + "knabbel.titel": ("Herinnering: de eerste kaasknabbel", "Memory: the first cheese nibble"),
    SCENE + "knabbel.naam.snoes": ("Kleine Snoes", "Little Snoes"),
    SCENE + "knabbel.k1": ("Lang geleden. Snoes was nog maar een heel klein guhtje.", "Long ago. Snoes was only a very little guh."),
    SCENE + "knabbel.k2": ("In het gras lag iets geels. Het rook naar kaas.", "In the grass lay something yellow. It smelled of cheese."),
    SCENE + "knabbel.k3": ("Is die... voor mij? Er staat geen naam op.", "Is that... for me? There's no name on it."),
    SCENE + "knabbel.k4": ("Hij wachtte. Misschien kwam iemand hem halen. Er kwam niemand.",
                           "He waited. Maybe someone would come for it. Nobody came."),
    SCENE + "knabbel.k5": ("*knabbel*... NJEG!", "*nibble*... NJEG!"),
    SCENE + "knabbel.k6": ("Zijn allereerste kaasknabbel. Niemand zag het. Maar hij is het nooit vergeten.",
                           "His very first cheese nibble. Nobody saw. But he never forgot it."),
    # 2. the first night with a Blub
    SCENE + "blub.titel": ("Herinnering: het eerste nachtje met een Blub", "Memory: the first night with a Blub"),
    SCENE + "blub.naam.snoes": ("Kleine Snoes", "Little Snoes"),
    SCENE + "blub.naam.blub": ("Blub", "Blub"),
    SCENE + "blub.b1": ("Een koude nacht. De wind floot door het gras.", "A cold night. The wind whistled through the grass."),
    SCENE + "blub.b2": ("Brrr. Iedereen heeft een plekje. Ik heb alleen wind.", "Brrr. Everyone has a place. I only have wind."),
    SCENE + "blub.b3": ("Toen kwam er iets zachts aanhuppelen. Het piepte heel zachtjes.",
                        "Then something soft came hopping along. It squeaked, very softly."),
    SCENE + "blub.b4": ("Blub.", "Blub."),
    SCENE + "blub.b5": ("De Blub kroop tegen hem aan, uit de wind. Hij zei niks. Hij bleef gewoon.",
                        "The Blub snuggled up to him, out of the wind. It said nothing. It just stayed."),
    SCENE + "blub.b6": ("Voor het eerst sliep Snoes. Een klein beetje maar. Toen hij wakker werd, was de Blub weg.",
                        "For the first time, Snoes slept. Only a little bit. When he woke up, the Blub was gone."),
    # 3. the first stargazing
    SCENE + "sterren.titel": ("Herinnering: het eerste sterrenkijken", "Memory: the first stargazing"),
    SCENE + "sterren.naam.snoes": ("Kleine Snoes", "Little Snoes"),
    SCENE + "sterren.s1": ("Een heldere nacht. Snoes lag in het gras en telde sterren.",
                           "A clear night. Snoes lay in the grass, counting stars."),
    SCENE + "sterren.s2": ("Een, twee... honderd... nog eentje... en nog eentje...", "One, two... a hundred... one more... and one more..."),
    SCENE + "sterren.s3": ("Toen zag hij het: een paar sterren samen. Twee oortjes. Een snoetje.",
                           "Then he saw it: a few stars together. Two little ears. A little snout."),
    SCENE + "sterren.s4": ("Een guh! Daarboven woont ook een guh. Helemaal alleen... net als ik.",
                           "A guh! There's a guh living up there too. All alone... just like me."),
    SCENE + "sterren.s5": ("Hij zwaaide. De sterren twinkelden terug.", "He waved. The stars twinkled back."),
    SCENE + "sterren.s6": ("Dan zijn we samen een beetje minder alleen. Welterusten, sterrenguh. Njeg.",
                           "Then together we're a little less alone. Good night, star guh. Njeg."),
    SCENE + "sterren.s7": ("Snoes had nergens een plekje. Maar vanaf die nacht wist hij: er is altijd wel iemand die terugtwinkelt.",
                           "Snoes had a place nowhere. But from that night on he knew: there's always someone who twinkles back."),
}


def _direct():
    blokken, mensen = build()
    en_pad = os.path.join("tools", "lang", "en", "c212_herinnering.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in TEKSTEN.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        for k in [k for k in d if any(k.startswith(SCENE + o + ".") for o in OUD_SCENES) or k in OUD_KEYS]:
            del d[k]
        d.update({k: v[i] for k, v in TEKSTEN.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    bad = [k for k, (nl, en) in TEKSTEN.items() if nl.count("%") != en.count("%")]
    if bad:
        raise SystemExit(f"herinnering: % mismatch {bad}")
    print(f"herinnering: island ({blokken} blocks, {mensen} figures), {len(BELLEN)} bellen, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
