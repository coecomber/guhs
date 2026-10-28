"""
1.8.0 Guhdromenland, island E "badje": het Wolkenbadje (dromen-ontwerp/BOUWPLAN_180.md part E; design render
dromen-ontwerp/_bron/r05_wolkenbadje.py, ontwerp-guhs180-dromen.md ronde 10: "bad klaarmaken"). Java: feature/droombadje
(DroombadjeFeature, Badje, BadjeOpzet, BadspulletjeBlock, BadjeScene, BadjeGameTests).

  build(h)    the island template data/guhs/structure/dromen/eiland_badje.nbt (stamped by DroomEilanden at compass 144),
              data/guhs/dromen/eiland/badje.json (part A's island json: midden, aankomst, steiger) and
              data/guhs/dromen/badje_route.json (this island's own data: the zeepbel route, its three stops with their
              badspulletje, Snoes' bath), the badspulletje block (badeend / zeepje / schuim), the signs and the texts
              (Dutch in nl_nl, English in tools/lang/en/c213_droombadje.json)

All positions in the jsons are relative to the island's centre block (the json "midden"), like part A's.

Standalone (from the project root): PYTHONPATH=tools python tools/features/droombadje.py
"""
import json
import math
import os
import random
import sys

from PIL import Image

# ---- layout (template coordinates; the render's scene coordinates, so the render stays a faithful picture) -------------
X, Y, Z = 100, 68, 96
C, CZ, T = 52, 52, 24                     # the island's centre column and its top (the "midden" block is (C, T, CZ))
BX, BZ, RX, RZ = C + 2, CZ + 4, 15, 9      # the giant tub
VLOER = T + 3
SX, SZ = C - 8, CZ + 22                    # Snoes' own little bath
STAP = (math.cos(math.radians(144)), math.sin(math.radians(144)))     # (unused helper of the render)
NAAR_PLEIN = (-math.sin(math.radians(144)), math.cos(math.radians(144)))  # from the island towards the plein (north-west)

# the three clouds with a badspulletje: (soort, centre of the flat cloud, which side you float in from)
STOPS = [("badeend", (C + 42, T + 10, CZ - 26), (-2.5, 1.5)),
         ("zeepje", (C + 44, T + 20, CZ + 12), (-2.5, -1.5)),
         ("schuim", (C + 2, T + 28, CZ + 38), (1.5, -2.0))]
START = (C + 21, T + 1, CZ - 13)            # the bubble pad (feet), by the "Zeepbelroute" sign
PREFIX = "sign.guhs.droombadje"

SPULLETJES = {  # soort -> (Dutch, English)
    "badeend": ("Badeend voor Snoes", "Rubber duck for Snoes"),
    "zeepje": ("Zeepje voor Snoes", "Soap for Snoes"),
    "schuim": ("Schuimfles voor Snoes", "Bubble bath for Snoes"),
}
P = "gui.guhs.droombadje."
SCENE = "droombadje_snoes_bad"
TEKSTEN = {  # key -> (Dutch, English)
    "block.guhs.droom_badspulletje": ("Badspulletje", "Bath thing"),
    P + "start": ("Plop! Een zeepbel neemt je mee. Sluipen = eruit springen.", "Plop! A bubble takes you along. Sneak = hop out."),
    P + "uit": ("Plop! De zeepbel is geknapt. Zachtjes naar beneden, vads...", "Plop! The bubble popped. Gently down, vads..."),
    P + "klaar": ("De zeepbel zet je zachtjes neer. Njeg, wat een ritje!", "The bubble sets you down gently. Njeg, what a ride!"),
    P + "stop": ("Klik op het badspulletje! (De zeepbel wacht even.)", "Click the bath thing! (The bubble waits a moment.)"),
    P + "pak": ("%s gevonden! (%s/3)", "%s found! (%s/3)"),
    P + "al": ("Die heb je al, vads. Op naar de volgende wolk!", "You already have that one, vads. On to the next cloud!"),
    P + "alle": ("Alle drie de badspulletjes! Breng ze naar Snoes bij het kleine badje.",
                 "All three bath things! Bring them to Snoes by the little bath."),
    P + "snoes.nog": ("Snoes: 'Een... bad? Ik heb nog nooit in bad gezeten. Zonder eendje en zeepje durf ik niet...' (%s/3)",
                      "Snoes: 'A... bath? I've never had a bath. Without a duck and soap I don't dare...' (%s/3)"),
    P + "snoes.al": ("Snoes: 'Ik ben nog nooit zo schoon geweest. Ik ruik naar bubbels! Njeg!'",
                     "Snoes: 'I've never been this clean. I smell of bubbles! Njeg!'"),
    P + "stukje": ("Een stukje van Snoes' droom: schoon en warm. (%s/5)", "A piece of Snoes' dream: clean and warm. (%s/5)"),
    # the cutscene (tools/features/verhaal_motor.scene: scene.guhs.<id>.<key>)
    f"scene.guhs.{SCENE}.titel": ("Snoes' eerste bubbelbad", "Snoes' first bubble bath"),
    f"scene.guhs.{SCENE}.naam.snoes": ("Snoes", "Snoes"),
    f"scene.guhs.{SCENE}.kijk": ("Is dat... allemaal voor mij? Een eendje, een zeepje en... schuim?",
                                 "Is that... all for me? A duck, a soap and... foam?"),
    f"scene.guhs.{SCENE}.teen": ("Eerst één teentje. Oeh. Warm!", "Just one toe first. Ooh. Warm!"),
    f"scene.guhs.{SCENE}.plons": ("PLONS!", "SPLASH!"),
    f"scene.guhs.{SCENE}.blub": ("Blub blub blub... ik ben een bubbel!", "Blub blub blub... I'm a bubble!"),
    f"scene.guhs.{SCENE}.dank": ("Ik heb nog nooit in bad gezeten. Dankjewel, vadsje.", "I've never had a bath before. Thank you, vadsje."),
    f"scene.guhs.{SCENE}.verteller": ("Snoes is schoon en warm. Een stukje van zijn droom komt terug.",
                                      "Snoes is clean and warm. A piece of his dream comes back."),
}
for s, (nl, en) in SPULLETJES.items():
    TEKSTEN[f"{P}spulletje.{s}"] = (nl, en)

# the signs: Dutch line -> English line (the keys come from tools/sign_text.py)
BORDEN_EN = {
    "Wolkenbadje": "Cloud Bath", "Eerst je pootjes": "Wipe your paws", "vegen, vads!": "first, vads!",
    "~ Zeepbelroute ~": "~ Bubble route ~", "Stap op de bel,": "Step on the bubble,", "zweef mee!": "float along!",
    "Niet prikken!": "No poking!", "Handdoekjes:": "Towels:", "Eigen handdoek?": "Own towel?", "Njeg, lenen mag": "Njeg, borrow one",
    "Snoes' badje": "Snoes' little bath", "Sst, hij durft": "Shh, he's still", "nog niet goed": "a bit scared",
    "Kraan:": "Tap:", "roze = warm": "pink = warm", "blauw = koud": "blue = cold", "~ Naar het ~": "~ To the ~",
    "~ Droomplein ~": "~ Dream Square ~", "stap erop": "step on it",
}


def rel(x, y, z):
    """Template coordinates -> relative to the centre block (what the jsons and the Java use)."""
    return [round(x - C, 2), round(y - T, 2), round(z - CZ, 2)]


# =====================================================================================================================
# the template
# =====================================================================================================================
def _bouw():
    sys.path.insert(0, os.path.join("tools", "features"))
    import onderwol_bouw as ob
    import make_structures as ms
    rng = random.Random(144)
    b = ob.Bouw((X, Y, Z), "droombadje", prefix=PREFIX)

    def zet(x, y, z, s, keep=False):
        x, y, z = int(round(x)), int(round(y)), int(round(z))
        if keep and (x, y, z) in b.blocks and b.blocks[(x, y, z)][0] != "minecraft:air":
            return
        b.zet(x, y, z, s)

    def ellipsoid(cx, cy, cz, rx, ry, rz, s, hollow=0.0, keep=False, ymax=None):
        for x in range(int(cx - rx - 1), int(cx + rx + 2)):
            for y in range(int(cy - ry - 1), int(cy + ry + 2)):
                if ymax is not None and y > ymax:
                    continue
                for z in range(int(cz - rz - 1), int(cz + rz + 2)):
                    d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2
                    if d > 1:
                        continue
                    if hollow and ((x - cx) / (rx - hollow)) ** 2 + ((y - cy) / (ry - hollow)) ** 2 + ((z - cz) / (rz - hollow)) ** 2 <= 1:
                        continue
                    zet(x, y, z, s, keep)

    def bel(cx, cy, cz, r):
        """A see-through soap bubble (decoration: never on the float route)."""
        ellipsoid(cx, cy, cz, r, r, r, "zeepbelglas", hollow=0.75, keep=True)

    def wolk(cx, cy, cz, r, kleur="wolkenblok_wit"):
        ellipsoid(cx, cy, cz, r, r * 0.55, r * 0.8, kleur, keep=True)
        for _ in range(4):
            ellipsoid(cx + rng.randint(-r, r), cy + rng.randint(0, 1), cz + rng.randint(-r // 2 - 1, r // 2 + 1),
                      r * 0.6, r * 0.45, r * 0.6, kleur if rng.random() < 0.7 else "wolkenblok_roze", keep=True)

    # ---- the island: a big cloud with a drop-shaped belly (droomkit.eiland) ------------------------------------------
    fase = [rng.random() * 2 * math.pi for _ in range(3)]
    R0, DIEPTE = 30, 18

    def rand(a):
        return R0 * (1 + 0.18 * (0.6 * math.sin(3 * a + fase[0]) + 0.4 * math.sin(5 * a + fase[1]) + 0.25 * math.sin(8 * a + fase[2])))

    def binnen(x, z):
        return math.hypot(x - C, z - CZ) <= rand(math.atan2(z - CZ, x - C))

    for x in range(X):
        for z in range(Z):
            dx, dz = x - C, z - CZ
            d = math.hypot(dx, dz) / rand(math.atan2(dz, dx))
            if d > 1:
                continue
            h = int(DIEPTE * (1 - d) ** 0.7 + rng.random() * 1.5) + 1
            for k in range(h + 1):
                blok = "wolkenblok_wit" if k <= 2 else "droomzandblok" if k >= h - 1 and d < 0.3 else "pluiswolblok"
                zet(x, T - k, z, blok)
            if d > 0.82 and rng.random() < 0.12:
                for k in range(1, rng.randint(2, 4)):
                    zet(x, T - h - k, z, "wolkenweide_rank[tip=false]")
    for a in range(0, 360, 14):                      # fluffy white and pink rim (open on the plein side for the steiger)
        if not (195 <= a <= 255):
            x, z = C + int(29 * math.cos(math.radians(a))), CZ + int(29 * math.sin(math.radians(a)))
            ellipsoid(x, T, z, 3.5, 2, 3, "wolkenblok_roze" if a % 28 else "wolkenblok_wit", keep=True)
    for x in range(X):                               # tiles round the tub
        for z in range(Z):
            if ((x - C) / 21) ** 2 + ((z - CZ - 2) / 15) ** 2 <= 1 and binnen(x, z):
                zet(x, T, z, "knuffelbad_badtegel" if (x + z) % 2 else "pleister_hemelsblauw")

    # ---- the giant tub on golden feet ---------------------------------------------------------------------------------
    for (dx, dz) in ((-11, -6), (11, -6), (-11, 6), (11, 6)):
        for y in range(T + 1, VLOER):
            zet(BX + dx, y, BZ + dz, "minecraft:gold_block")
    for x in range(BX - RX - 1, BX + RX + 2):
        for z in range(BZ - RZ - 1, BZ + RZ + 2):
            if ((x - BX) / RX) ** 2 + ((z - BZ) / RZ) ** 2 > 1:
                continue
            zet(x, VLOER, z, "minecraft:smooth_quartz")
            kuip = ((x - BX) / (RX - 1.2)) ** 2 + ((z - BZ) / (RZ - 1.2)) ** 2 <= 1
            for y in range(VLOER + 1, VLOER + 6):
                zet(x, y, z, ("minecraft:water" if y < VLOER + 5 else "minecraft:air") if kuip else "minecraft:smooth_quartz")
            if not kuip:
                b.set(x, VLOER + 6, z, "minecraft:smooth_quartz_slab", {"type": "bottom", "waterlogged": "false"})
            elif rng.random() < 0.38:
                zet(x, VLOER + 5, z, "knuffelbad_schuim")
    for dx in (-9, -3, 3, 9):
        zet(BX + dx, VLOER + 3, BZ + RZ, "tegel_guhgezicht")
        zet(BX + dx, VLOER + 3, BZ - RZ, "tegel_guhgezicht")
    for (dx, dy, dz, r) in ((-6, 9, -2, 1), (2, 11, 3, 2), (8, 8, -3, 1), (-9, 12, 4, 1)):
        bel(BX + dx, VLOER + dy, BZ + dz, r)
    # the tap: a silver pipe with a warm and a cold knob, a stream of bubble water into the tub
    KX, KZ = BX + RX + 1, BZ
    for y in range(VLOER + 1, VLOER + 11):
        zet(KX + 1, y, KZ, "minecraft:light_gray_concrete")
    for x in range(KX - 3, KX + 2):
        zet(x, VLOER + 10, KZ, "minecraft:light_gray_concrete")
    zet(KX - 3, VLOER + 9, KZ, "minecraft:light_gray_concrete")
    zet(KX + 1, VLOER + 11, KZ - 1, "pink_kussen[facing=north]")
    zet(KX + 1, VLOER + 11, KZ + 1, "light_blue_kussen[facing=north]")
    for y in range(VLOER + 5, VLOER + 9):
        zet(KX - 3, y, KZ, "minecraft:light_blue_stained_glass")
    b.sign(KX + 2, VLOER + 2, KZ, "east", ["Kraan:", "roze = warm", "blauw = koud", ""])
    # cloud stairs up to the tub's rim (the plein side)
    for k in range(4):
        for x in range(BX - 4 + k, BX + 5 - k):
            zet(x, T + 1 + k, BZ - RZ - 4 + k, "wolkenblok_wit_trap[facing=south,half=bottom,shape=straight,waterlogged=false]")

    # ---- the towel rack ------------------------------------------------------------------------------------------------
    HX, HZ = C - 20, CZ + 10
    for dz in (0, 6):
        for y in range(T + 1, T + 6):
            zet(HX, y, HZ + dz, "minecraft:iron_bars")
    for dz in range(0, 7):
        zet(HX, T + 5, HZ + dz, "minecraft:iron_bars")
    for dz, doek in ((1, "campingmarkt_tentdoek_blauw"), (2, "campingmarkt_tentdoek_blauw"), (4, "campingmarkt_tentdoek_creme"),
                     (5, "campingmarkt_tentdoek_creme")):
        zet(HX, T + 3, HZ + dz, doek)
        zet(HX, T + 4, HZ + dz, doek)
    b.sign(HX + 1, T + 1, HZ + 7, "east", ["Handdoekjes:", "Eigen handdoek?", "Njeg, lenen mag", ""], wall=False)

    # ---- Snoes' own little bath ----------------------------------------------------------------------------------------
    for x in range(SX - 3, SX + 4):
        for z in range(SZ - 2, SZ + 3):
            zet(x, T + 1, z, "knuffelbad_badtegel")
            zet(x, T + 2, z, "knuffelbad_badtegel")
    for x in range(SX - 2, SX + 3):
        for z in range(SZ - 1, SZ + 2):
            zet(x, T + 2, z, "minecraft:water")
    for (dx, dz) in ((-2, -1), (2, 0), (1, -1)):
        zet(SX + dx, T + 3, SZ + dz, "knuffelbad_schuim")
    bel(SX + 1, T + 8, SZ - 1, 1)
    zet(SX - 4, T + 1, SZ + 2, "white_kussen[facing=north]")
    zet(SX + 4, T + 1, SZ - 2, "sterrenlantaarn")
    b.sign(SX - 4, T + 1, SZ - 2, "north", ["Snoes' badje", "Sst, hij durft", "nog niet goed", ""], wall=False)
    snoes = (SX + 0.5, T + 1, SZ + 3.5)            # Snoes stands by his bath, looking at it (north)
    b.entity(snoes[0], snoes[1], snoes[2], {"id": "guhs:guh_npc", "Kind": "snoes", "PersistenceRequired": ms.Byte(1),
                                             "Invulnerable": ms.Byte(1), "Rotation": ms.floats(180.0, 0.0),
                                             "RoleData": {"guhs_droombadje": ms.Byte(1)}})

    # ---- the bubble pad and the three clouds with the bath things --------------------------------------------------------
    for x in range(START[0] - 1, START[0] + 2):
        for z in range(START[2] - 1, START[2] + 2):
            zet(x, T, z, "zeepbelglas")
    zet(START[0], T, START[2], "tegel_guhgezicht")
    b.sign(START[0] - 2, T + 1, START[2] + 3, "west", ["~ Zeepbelroute ~", "Stap op de bel,", "zweef mee!", "Niet prikken!"], wall=False)
    stops = []
    for soort, (cx, cy, cz), (hx, hz) in STOPS:
        # a flat cloud (its top at cy + 1) so you can stand on it; the bath thing in the middle; a big deco thing behind
        ellipsoid(cx, cy, cz, 4.5, 1.6, 3.5, "wolkenblok_wit", ymax=cy + 1)
        ellipsoid(cx, cy - 1, cz, 3.5, 1.4, 2.5, "wolkenblok_roze", keep=True)
        b.set(cx, cy + 2, cz, "guhs:droom_badspulletje", {"soort": soort})
        ax, az = cx - int(math.copysign(2, hx)), cz - int(math.copysign(2, hz))   # behind it, away from where you float in
        if soort == "badeend":
            ellipsoid(ax, cy + 3, az, 2, 1.4, 1.4, "minecraft:yellow_wool")
            ellipsoid(ax - 1, cy + 5, az, 1.1, 1.1, 1.1, "minecraft:yellow_wool")
            zet(ax - 2, cy + 5, az, "minecraft:orange_wool")
            zet(ax - 1, cy + 6, az - 1, "pink_kussen[facing=north]")
            zet(ax - 1, cy + 6, az + 1, "pink_kussen[facing=north]")
        elif soort == "zeepje":
            for x in range(ax - 1, ax + 2):
                for y in range(cy + 2, cy + 4):
                    for z in range(az - 1, az + 1):
                        zet(x, y, z, "minecraft:pink_concrete")
            zet(ax, cy + 3, az + 1, "tegel_guhgezicht")
            for (dx, dy, dz) in ((-1, 5, 0), (1, 6, 0)):
                zet(ax + dx, cy + dy, az + dz, "zeepbel")
        else:
            for y in range(cy + 2, cy + 7):
                zet(ax, y, az, "minecraft:light_blue_stained_glass" if y > cy + 5 else "knuffelbad_schuim")
                for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    zet(ax + dx, y, az + dz, "minecraft:light_blue_stained_glass")
            zet(ax, cy + 7, az, "minecraft:light_blue_stained_glass")
            zet(ax, cy + 8, az, "pink_kussen[facing=north]")
        stops.append({"soort": soort, "spulletje": rel(cx, cy + 2, cz), "hover": rel(cx + hx + 0.5, cy + 2, cz + hz + 0.5)})

    # ---- the steiger to the plein (north-west), the welcome sign, lamps ---------------------------------------------------
    nx, nz = NAAR_PLEIN
    for k10 in range(220, 380, 5):
        k = k10 / 10
        for w in (-1.5, -0.5, 0.5, 1.5):
            x = int(round(C + nx * k - nz * w))
            z = int(round(CZ + nz * k + nx * w))
            if 0 <= x < X and 0 <= z < Z:
                zet(x, T, z, "pleister_hemelsblauw" if not binnen(x, z) or k > 28 else "tegel_guhgezicht")
    steiger = (C + nx * 36.5, T + 1, CZ + nz * 36.5)
    bel(int(C + nx * 39), T + 5, int(CZ + nz * 39), 3)       # the big bubble at the end (decoration, beyond the steiger spot)
    for s in (1, -1):
        zet(int(C + nx * 30 - nz * 2.5 * s), T + 1, int(CZ + nz * 30 + nx * 2.5 * s), "lampion_mint[hanging=false]")
    b.sign(int(C + nx * 33 - nz * 2.5), T + 1, int(CZ + nz * 33 + nx * 2.5), "south", ["~ Naar het ~", "~ Droomplein ~", "stap erop", ""], wall=False)
    b.sign(int(C + nx * 24 + 3), T + 1, int(CZ + nz * 24), "south", ["Wolkenbadje", "Eerst je pootjes", "vegen, vads!", ""], wall=False)
    aankomst = (C + nx * 25, T + 1, CZ + nz * 25)

    # ---- lamps, Blubs, ducks, waiting guhs -------------------------------------------------------------------------------
    for a in range(20, 360, 45):
        x, z = C + int(24 * math.cos(math.radians(a))), CZ + int(22 * math.sin(math.radians(a)))
        if binnen(x, z) and (x, T + 1, z) not in b.blocks:
            zet(x, T + 1, z, "wolkenlamp")
    for (x, z, yaw, soort, meter, strik) in ((C - 14, CZ - 6, 60, "blauw", 34, None), (C + 20, CZ + 16, 300, "weide", 52, None),
                                             (SX + 4, SZ + 1, 250, "blauw", 61, "strik_pink")):
        nbt = {"id": "guhs:blubguh", "BlubSoort": soort, "Meter": meter, "Tint": 0, "Grootte": ms.Float(1.0),
               "PersistenceRequired": ms.Byte(1), "Rotation": ms.floats(float(yaw), 0.0)}
        if strik:
            nbt["Kleding"] = {"strik": strik}
        b.entity(x + 0.5, T + 1, z + 0.5, nbt)
    for (dx, dz, soort) in ((-7, 2, 3), (0, -3, 0), (6, 3, 9), (-3, 5, 1)):   # GUHEENDJE, NORMAAL, MAANEENDJE, VADSEENDJE
        b.entity(BX + dx + 0.5, VLOER + 4.6, BZ + dz + 0.5, {"id": "guhs:badeendje", "Soort": ms.Byte(soort), "Deco": ms.Byte(1),
                                                              "Rotation": ms.floats(float(rng.randint(0, 359)), 0.0)})
    b.guh(C - 18.5, T + 1, CZ + 2.5, yaw=90)
    b.guh(C + 14.5, T + 1, CZ - 12.5, yaw=230)
    zet(C - 20, T + 1, CZ + 3, "knuffelsteen_gezicht[facing=east,stemming=1]")

    b.save("dromen/eiland_badje")
    route = _route(stops)
    return {"stops": stops, "route": route, "snoes": rel(*snoes), "bad": rel(SX + 0.5, T + 2.2, SZ + 0.5), "start": rel(*START),
            "steiger": rel(*steiger), "aankomst": rel(*aankomst)}


def _route(stops):
    """The float path (relative feet positions), with the index of each stop's hover point."""
    def r(x, y, z):
        return rel(C + x, T + y, CZ + z)
    s1, s2, s3 = (st["hover"] for st in stops)

    def boven(h):
        """Just above a stop: the bubble comes down onto the cloud from above (never through it)."""
        return [h[0], round(h[1] + 3, 2), h[2]]
    punten = [r(21, 6, -14), r(30, 12, -22), boven(s1), s1, boven(s1), r(46, 18, -12), r(48, 24, 4), boven(s2), s2, boven(s2),
              r(34, 28, 24), r(16, 32, 34), boven(s3), s3, boven(s3), r(4, 24, 30), r(-2, 12, 24), r(-4, 6, 19)]
    return punten


# =====================================================================================================================
# the block, the jsons, the texts
# =====================================================================================================================
def _tex(h, naam, kleur, rand=None, stip=None):
    img = Image.new("RGBA", (16, 16), kleur)
    px = img.load()
    rnd = random.Random(naam)
    for x in range(16):
        for y in range(16):
            r, g, b_, a = kleur
            v = rnd.randint(-8, 8)
            px[x, y] = (max(0, min(255, r + v)), max(0, min(255, g + v)), max(0, min(255, b_ + v)), a)
            if rand and (x in (0, 15) or y in (0, 15)):
                px[x, y] = rand
    if stip:
        for (x, y) in ((5, 5), (6, 5), (10, 5), (11, 5)):
            px[x, y] = stip
    pad = os.path.join(h.A, "textures", "block", f"{naam}.png")
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    img.save(pad)


def _el(frm, to, tex):
    return {"from": frm, "to": to, "faces": {f: {"texture": f"#{tex}"} for f in ("north", "south", "east", "west", "up", "down")}}


MODELLEN = {
    "badeend": ({"lijf": "guhs:block/droom_badspulletje_geel", "snavel": "guhs:block/droom_badspulletje_oranje",
                 "oor": "guhs:block/droom_badspulletje_roze"},
                [_el([4, 0, 4], [12, 5, 12], "lijf"), _el([5, 5, 6], [10, 10, 10], "lijf"), _el([3, 6, 7], [5, 8, 9], "snavel"),
                 _el([5, 10, 5], [7, 12, 7], "oor"), _el([5, 10, 9], [7, 12, 11], "oor"), _el([11, 3, 7], [13, 6, 9], "lijf")]),
    "zeepje": ({"zeep": "guhs:block/droom_badspulletje_zeep", "bel": "guhs:block/droom_badspulletje_bel"},
               [_el([3, 0, 4], [13, 4, 12], "zeep"), _el([6, 5, 6], [8, 7, 8], "bel"), _el([9, 7, 8], [11, 9, 10], "bel")]),
    "schuim": ({"fles": "guhs:block/droom_badspulletje_fles", "dop": "guhs:block/droom_badspulletje_roze",
                "schuim": "guhs:block/droom_badspulletje_schuim"},
               [_el([5, 0, 5], [11, 9, 11], "fles"), _el([6, 9, 6], [10, 11, 10], "fles"), _el([6, 11, 6], [10, 13, 10], "dop"),
                _el([6, 1, 6], [10, 6, 10], "schuim")]),
}


def blok(h):
    _tex(h, "droom_badspulletje_geel", (255, 222, 70, 255), stip=(40, 30, 40, 255))
    _tex(h, "droom_badspulletje_oranje", (255, 150, 50, 255))
    _tex(h, "droom_badspulletje_roze", (255, 160, 200, 255))
    _tex(h, "droom_badspulletje_zeep", (255, 182, 214, 255), rand=(240, 140, 190, 255), stip=(60, 40, 70, 255))
    _tex(h, "droom_badspulletje_bel", (200, 236, 255, 170))
    _tex(h, "droom_badspulletje_fles", (180, 225, 255, 150), rand=(140, 200, 240, 200))
    _tex(h, "droom_badspulletje_schuim", (250, 250, 255, 255))
    for soort, (tex, els) in MODELLEN.items():
        h.w(f"{h.A}/models/block/droom_badspulletje_{soort}.json",
            {"parent": "minecraft:block/block", "textures": dict(tex, particle=next(iter(tex.values()))), "elements": els})
    h.w(f"{h.A}/blockstates/droom_badspulletje.json",
        {"variants": {f"soort={s}": {"model": f"guhs:block/droom_badspulletje_{s}"} for s in MODELLEN}})


def build(h):
    import sign_text
    plan = _bouw()
    blok(h)
    h.w(f"{h.D}/dromen/eiland/badje.json", {
        "versie": 1, "template": "guhs:dromen/eiland_badje", "midden": [C, T, CZ],
        "aankomst": {"plek": plan["aankomst"], "yaw": round(-math.degrees(math.atan2(-NAAR_PLEIN[0], -NAAR_PLEIN[1])), 1)},
        "steiger": plan["steiger"]})
    h.w(f"{h.D}/dromen/badje_route.json", {"start": plan["start"], "route": plan["route"],
                                           "stops": [{"soort": s["soort"], "spulletje": s["spulletje"], "hover": s["hover"],
                                                      "index": plan["route"].index(s["hover"])} for s in plan["stops"]],
                                           "snoes": plan["snoes"], "bad": plan["bad"]})
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)
    borden = {k: v for k, v in sign_text.TEXTS.items() if k.startswith(PREFIX + ".")}
    weg = [v for v in borden.values() if v not in BORDEN_EN]
    if weg:
        raise SystemExit(f"droombadje: signs without English: {weg}")
    return borden


def _direct():
    sys.path.insert(0, "tools")
    import make_v2 as h
    borden = build(h)
    alles = {k: v for k, v in TEKSTEN.items()}
    alles.update({k: (nl, BORDEN_EN[nl]) for k, nl in borden.items()})
    en_pad = os.path.join("tools", "lang", "en", "c213_droombadje.json")
    with open(en_pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump({k: en for k, (nl, en) in alles.items()}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update({k: v[i] for k, v in alles.items()})
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"droombadje: island template, route ({len(STOPS)} stops), badspulletje block, {len(alles)} texts")


if __name__ == "__main__":
    _direct()
