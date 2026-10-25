"""
1.7.0 "Onder de Wol": the buildings of the two cave minigames of tools/features/mijnspellen.py (Java: feature/mijnspellen).

Each game is three templates, chained by vertical jigsaws (the hall is the start piece; its top jigsaw is the structure's
start_jigsaw_name, projected to the surface minus the shaft, so the hut lands on the ground):

  <game>_zaal     the cave hall: walls, floor and ceiling all round (it carves its own room, caves can't run into it),
                  the game's marker blocks, the NPC, the Wolkenliftje (bottom) and a jigsaw in the ceiling above it;
                  (1.7.0 playtest) with spelhal_bouw.RAND blocks of the world's rock round it and three extra entrances
                  (a door, a short tunnel, a chamber with "Ingang" signs): only the hall itself is protected (Java Spelhal);
  <game>_schacht  a 1x1 column of nothing (structure void) as tall as SCHACHT: only two jigsaws, so the rock between the
                  hall and the hut stays rock (and isn't protected);
  <game>_hut      the little entrance hut on the surface with a sign and the Wolkenliftje (top), straight above the bottom
                  one (the Wolkenliftje only goes straight up/down), on spelhal_bouw foundations; worldgen puts it on the
                  ground at its own spot ("hut_op_grond").

The rail networks, bins and levers of the Wisselgrot are NOT in the template: Conducteur Piepwiel lays them from
KarretjesNet.java when a shift starts (one source of truth); this module only keeps the floor and the balcony free for
them (KAR_BANEN, checked against the Java file). The bowling lanes ARE built here (lane, gutters, bumpers, posts), with
the same numbers as KegelBaan.java (checked too).

(ronde 45-47, spelhal_kracht.py) Both halls run on vadskracht, early tier, the games never depending on it:
  the Wisselgrot   a Guhrad at the balcony's east end: the balcony's lights (Vadslampen instead of the hanging lanterns)
                   and two scoreboard lamps by Piepwiel (10 VK); the lanterns over the rail network stay
  the Kegelgrot    two Guhraden: one in the lounge behind De Guhde (the lounge's lights and two scoreboard lamps on his
                   counter, 6 VK) and the KEGELZETTER: a bridge over the end of the lanes (u ZETTER_U, a block above the
                   pins) with a Guhrad on it and a Vadslamp per lane (3 VK), and a sign per lane (u ZETTER_BORD_U) that
                   Java (mijnspellen/kegelgrot/Kegelzetter.java, the same numbers, checked) turns to "pauze" while the
                   setter has no vadskracht; the pins are set all the same.
"""
import json
import math
import os
import re

from features import spelhal_bouw as sh
from features import spelhal_kracht as sk

SCHACHT = 20                 # the empty column between the hall's ceiling and the hut's floor
PLAFOND = "guhs:gepolijste_viltsteen"   # the Wisselgrot's ceiling
# --- Karretjes-wissels: the three networks (KarretjesNet.BANEN): switches (u, v), bins (u, v, ore) ---------------------
KAR_BANEN = [
    {"niveau": "makkelijk", "wissels": [(2, 0), (5, -3)], "bakken": [(8, -5, "kolen"), (8, -1, "ijzer"), (8, 3, "vads")]},
    {"niveau": "medium", "wissels": [(2, 0), (5, -4), (5, 4)],
     "bakken": [(9, -6, "kolen"), (9, -2, "goud"), (9, 2, "vads"), (9, 6, "ijzer")]},
    {"niveau": "lastig", "wissels": [(2, 0), (5, -5), (8, -7), (5, 5), (8, 7)],
     "bakken": [(11, -9, "kolen"), (11, -5, "diamant"), (11, -2, "goud"), (11, 2, "kaasknabbel"), (11, 5, "ijzer"), (11, 9, "vads")]},
]
ERTSEN = ["kolen", "ijzer", "goud", "vads", "kaasknabbel", "diamant"]          # (KarretjesNet.Erts, in order)
ERTS_BLOK = {"kolen": "guhs:kolenerts_vilt", "ijzer": "guhs:ijzererts_vilt", "goud": "guhs:gouderts_vilt",
             "vads": "guhs:vadserts_vilt", "kaasknabbel": "guhs:kaasknabbelerts_vilt", "diamant": "guhs:diamanterts_vilt"}
KAR_Y = 1                    # the hall's floor (y 0 is the foundation)
KAR_Z0 = 7                   # u = 0 (the tunnel mouths, the markers) is z 7; the balcony is z 1..6 (u -6..-1)
KAR_HOOG = 11                # the ceiling's y

# --- the Kegelgrot (KegelBaan.java) ---------------------------------------------------------------------------------------
KEG_Y = 1
KEG_Z0 = 12                  # u = 0 (the foul lines) is z 12; the approach u -7..-1, the lounge z 1..4
KEG_HOOG = 8
KEG_LENGTE = 15              # lane cells u 0..15, the pit u 16, the curtain u 17

JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "mijnspellen")


def _java(name):
    path = os.path.join(JAVA, name)
    return open(path, encoding="utf-8").read() if os.path.exists(path) else None


def kegel_getallen():
    """The numbers of KegelBaan.java this module builds with (read from the file, so they can't drift)."""
    src = _java(os.path.join("kegelgrot", "KegelBaan.java"))
    out = {"HALF": 1.5, "BOCHT": 1.0, "BOCHT_VAN": 2.5, "BOCHT_TOT": 11.0, "PAAL_U": 7.0, "PAAL_V": 1.0, "PUT": 15.5}
    if src:
        for key in out:
            m = re.search(rf"\b{key} = (-?[0-9.]+)", src)
            if m:
                out[key] = float(m.group(1))
    return out


def check_java():
    """KAR_BANEN = KarretjesNet.java (switches and bins per network, in the order the Java builds them); the Kegelzetter's
    numbers = Kegelzetter.java."""
    problems = []
    zet = _java(os.path.join("kegelgrot", "Kegelzetter.java"))
    if zet is not None:
        for naam, waarde in (("U", ZETTER_U), ("BORD_U", ZETTER_BORD_U), ("DY", ZETTER_DY)):
            m = re.search(rf"\b{naam} = (-?\d+)", zet)
            if not m or int(m.group(1)) != waarde:
                problems.append(f"Kegelzetter.{naam} != {waarde}")
    src = _java(os.path.join("karretjes", "KarretjesNet.java"))
    if src is None:
        return problems
    delen = src.split("banen.add(baan(Niveau.")[1:]
    if len(delen) != 3:
        return [f"KarretjesNet.java: expected 3 networks, found {len(delen)}"]
    for deel, baan in zip(delen, KAR_BANEN):
        if not deel.startswith(baan["niveau"].upper()):
            problems.append(f"network order: {deel[:10]} vs {baan['niveau']}")
        wissels = sorted((int(a), int(b)) for a, b in re.findall(r"wissel\((-?\d+), (-?\d+),", deel))
        bakken = sorted((int(a), int(b), c.lower()) for a, b, c in re.findall(r"bak\((-?\d+), (-?\d+), Erts\.(\w+)\)", deel))
        if wissels != sorted(baan["wissels"]):
            problems.append(f"{baan['niveau']}: switches {wissels} != {sorted(baan['wissels'])}")
        if bakken != sorted(baan["bakken"]):
            problems.append(f"{baan['niveau']}: bins {bakken} != {sorted(baan['bakken'])}")
    return problems


def _extent(baan):
    vs = [b[1] for b in baan["bakken"]]
    return min(vs), max(vs), max(b[0] for b in baan["bakken"])


# =====================================================================================================================
# small helpers
# =====================================================================================================================
YAW = {"south": 0.0, "west": 90.0, "north": 180.0, "east": -90.0}


def npc(ms, s, x, y, z, kind, kijk="south"):
    s.entity(x + 0.5, float(y), z + 0.5, {"id": "guhs:guh_npc", "Kind": kind, "PersistenceRequired": ms.Byte(1),
                                          "Rotation": ms.floats(YAW[kijk], 0.0)})


def bord(ms, s, x, y, z, facing, lines, texts, prefix, wall=True, hout="spruce"):
    """A sign (wall sign facing `facing`, or a standing one with rotation), its lines as translate keys."""
    msgs = []
    for i, line in enumerate((list(lines) + [None] * 4)[:4]):
        if not line:
            msgs.append(json.dumps(""))
            continue
        nl, en = line
        key = f"sign.guhs.{prefix}_{i}"
        texts[key] = (en, nl)
        msgs.append(json.dumps({"translate": key, "fallback": nl}))
    nbt = {"id": "minecraft:sign",
           "front_text": {"messages": ms.NbtList(8, msgs), "color": "black", "has_glowing_text": ms.Byte(0)},
           "back_text": {"messages": ms.NbtList(8, [json.dumps("")] * 4), "color": "black", "has_glowing_text": ms.Byte(0)},
           "is_waxed": ms.Byte(1)}
    if wall:
        s.set(x, y, z, f"minecraft:{hout}_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
    else:
        s.set(x, y, z, f"minecraft:{hout}_sign", {"rotation": str(facing), "waterlogged": "false"}, nbt)


def jigsaw(s, x, y, z, richting, naam, doel, pool, final):
    s.set(x, y, z, "minecraft:jigsaw", {"orientation": f"{richting}_north"},
          {"id": "minecraft:jigsaw", "name": naam, "target": doel, "pool": pool, "joint": "rollable",
           "final_state": final, "selection_priority": 0, "placement_priority": 0})


def doos(s, x0, y0, z0, x1, y1, z1, wand, vloer, plafond, binnen="minecraft:air"):
    """A closed box: walls, floor, ceiling, air inside."""
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                rand = x in (x0, x1) or z in (z0, z1)
                if y == y0:
                    s.set(x, y, z, vloer)
                elif y == y1:
                    s.set(x, y, z, plafond)
                elif rand:
                    s.set(x, y, z, wand)
                else:
                    s.set(x, y, z, binnen)


# =====================================================================================================================
# the Wisselgrot (Karretjes-wissels)
# =====================================================================================================================
def karretjes_zaal(ms, texts, rng):
    """Returns (Structure, info)."""
    breedtes = []
    xs = 1
    for baan in KAR_BANEN:
        vmin, vmax, umax = _extent(baan)
        w = vmax - vmin + 5
        breedtes.append((xs, w, xs + vmax + 2, umax))      # (x start, width, mx: the x of v = 0, uMax)
        xs += w + 1
    X, Z, Y = xs + 0, KAR_Z0 + 14, KAR_HOOG + 1
    S = ms.Structure((X, Y, Z))
    rots = "guhs:viltsteen"
    # (the ceiling is NOT plain viltsteen: the cave decoration hangs glitterrank from viltsteen ceilings, right over the levers)
    doos(S, 0, 0, 0, X - 1, KAR_HOOG, Z - 1, rots, "guhs:ertsgruissteen", PLAFOND)
    for x in range(X):                                    # the foundation and the floor
        for z in range(Z):
            S.set(x, 0, z, rots)
            S.set(x, KAR_Y, z, "guhs:ertsgruissteen")
    # ore veins and brick bands in the walls
    for x in range(X):
        for z in range(Z):
            for y in range(1, KAR_HOOG):
                if (x in (0, X - 1) or z in (0, Z - 1)) and rng.random() < 0.07:
                    S.set(x, y, z, ERTS_BLOK[rng.choice(ERTSEN)])
                if (x in (0, X - 1) or z in (0, Z - 1)) and y == 4:
                    S.set(x, y, z, "guhs:ertsgruis_bakstenen")
    # under the balcony (u -6..-1, z 1..6): solid up to the balcony floor, the tunnels cut in
    for x in range(1, X - 1):
        for z in range(1, KAR_Z0):
            for y in range(KAR_Y, KAR_Y + 4):
                S.set(x, y, z, rots)
            S.set(x, KAR_Y + 4, z, "guhs:bielzenhout")                 # the balcony floor
            for y in range(KAR_Y + 5, KAR_HOOG):
                S.set(x, y, z, "minecraft:air")
    info = {"banen": [], "lift": None, "npc": None}
    for b, (baan, (x0, w, mx, umax)) in enumerate(zip(KAR_BANEN, breedtes)):
        vmin, vmax, _ = _extent(baan)
        # the separator after this baan (a low wall of sleepers with a fence)
        sep = x0 + w
        if sep < X - 1:
            for z in range(KAR_Z0, Z - 1):
                S.set(sep, KAR_Y + 1, z, "guhs:bielzenhout")
                S.set(sep, KAR_Y + 2, z, "minecraft:spruce_fence", {"east": "false", "west": "false", "north": "true", "south": "true",
                                                                    "waterlogged": "false"})
        # the tunnel: u -3..-1 at v 0, two high; behind it (u -4) a dark tunnel end
        for u in (-3, -2, -1):
            z = KAR_Z0 + u
            S.set(mx, KAR_Y, z, "guhs:bielzenhout")
            S.set(mx, KAR_Y + 1, z, "minecraft:air")
            S.set(mx, KAR_Y + 2, z, "minecraft:air")
        S.set(mx, KAR_Y + 1, KAR_Z0 - 4, "minecraft:black_concrete")
        S.set(mx, KAR_Y + 2, KAR_Z0 - 4, "minecraft:black_concrete")
        for dx in (-1, 1):                                                # a sleeper frame round the tunnel mouth
            for y in (KAR_Y + 1, KAR_Y + 2):
                S.set(mx + dx, y, KAR_Z0 - 1, "minecraft:stripped_spruce_log", {"axis": "y"})
        S.set(mx, KAR_Y + 3, KAR_Z0 - 1, "minecraft:stripped_spruce_log", {"axis": "x"})
        # the marker: the tunnel mouth in the floor, carts roll south (+z), right = west (-x)
        S.set(mx, KAR_Y, KAR_Z0, "guhs:karretjes_spoorbegin", {"facing": "south", "baan": str(b)})
        # the railing (u = -1, a block above the balcony floor) all along this baan; the levers come on top of it
        for x in range(x0, x0 + w):
            S.set(x, KAR_Y + 5, KAR_Z0 - 1, "guhs:bielzenhout")
        # lights over the network
        for u in range(1, umax + 1, 4):
            for v in (vmin, 0, vmax):
                S.set(mx - v, KAR_HOOG - 1, KAR_Z0 + u, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
        # the labels: behind every bin the ore itself in the wall, and a bit of its colour
        for (u, v, erts) in baan["bakken"]:
            S.set(mx - v, KAR_Y + 2, Z - 1, ERTS_BLOK[erts])
            S.set(mx - v, KAR_Y + 1, Z - 1, ERTS_BLOK[erts])
            S.set(mx - v, KAR_Y + 3, Z - 1, "minecraft:glowstone")
        # a sign on the back wall of the balcony, in the middle of the baan
        niveau = baan["niveau"]
        regels = {"makkelijk": [("~ Baan 1 ~", "~ Track 1 ~"), ("Makkelijk", "Easy"), ("2 wissels,", "2 switches,"), ("3 bakken", "3 bins")],
                  "medium": [("~ Baan 2 ~", "~ Track 2 ~"), ("Medium", "Medium"), ("3 wissels,", "3 switches,"), ("4 bakken", "4 bins")],
                  "lastig": [("~ Baan 3 ~", "~ Track 3 ~"), ("Lastig!", "Hard!"), ("5 wissels,", "5 switches,"), ("6 bakken. Vads!", "6 bins. Vads!")]}
        bord(ms, S, mx, KAR_Y + 7, 1, "south", regels[niveau], texts, f"karretjes.baan_{niveau}")
        info["banen"].append({"mx": mx, "x0": x0, "w": w, "umax": umax})
        # keep the network's floor and the air above free (the Java lays rails and bins here)
        for u in range(0, umax + 2):
            for v in range(vmin - 1, vmax + 2):
                S.set(mx - v, KAR_Y, KAR_Z0 + u, "guhs:ertsgruissteen")
                for y in (KAR_Y + 1, KAR_Y + 2):
                    S.set(mx - v, y, KAR_Z0 + u, "minecraft:air")
        S.set(mx, KAR_Y, KAR_Z0, "guhs:karretjes_spoorbegin", {"facing": "south", "baan": str(b)})
    # a ladder from the floor back up to the balcony (in the free column at the far left of the first baan)
    lx = breedtes[0][0]
    for y in range(KAR_Y + 1, KAR_Y + 5):
        S.set(lx, y, KAR_Z0, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    S.set(lx, KAR_Y + 5, KAR_Z0 - 1, "minecraft:air")
    # the balcony: the Wolkenliftje, Conducteur Piepwiel, deco
    mid = X // 2
    lift = (mid - 3, KAR_Y + 4, 2)
    S.set(*lift, "guhs:wolkenliftje")
    jigsaw(S, lift[0], KAR_HOOG, lift[2], "up", "guhs:karretjes_lift", "guhs:karretjes_schacht_onder", "guhs:karretjeswissels/schacht",
           PLAFOND)
    info["lift"] = lift
    npc(ms, S, mid, KAR_Y + 5, 2, "wisselguh", "south")
    info["npc"] = (mid, KAR_Y + 5, 2)
    S.set(mid + 2, KAR_Y + 5, 1, "guhs:stationsklokje", {"facing": "south"})
    S.set(mid - 6, KAR_Y + 5, 1, "guhs:ertskarretje", {"facing": "east"})
    S.set(mid + 6, KAR_Y + 5, 1, "guhs:seinlampje", {"facing": "south"})
    S.set(mid - 9, KAR_Y + 5, 1, "guhs:seinlampje", {"facing": "south"})
    bord(ms, S, mid - 1, KAR_Y + 6, 1, "south", [("Wisselgrot", "Switch Cave"), ("Zet de wissels", "Set the switches"),
                                                ("met de hendels!", "with the levers!"), ("Aan = rechtsaf", "On = turn right")],
         texts, "karretjes.uitleg")
    # (ronde 45-47) the lamps along the balcony are Vadslampen on the balcony floor (karretjes_kracht)
    # (1.7.0 playtest) how it works, on the balcony wall: the levers, the bins, the calm start
    uitleg = [(mid + 2, [("~ De hendels ~", "~ The levers ~"), ("UIT: <- links", "OFF: <- left"), ("AAN: rechts ->", "ON: right ->"),
                         ("De rails wijzen!", "Rails show it!")]),
              (mid + 3, [("~ De bakken ~", "~ The bins ~"), ("Het erts in de", "The ore in the"), ("muur achter een", "wall behind a"),
                         ("bak = zijn erts!", "bin = its ore!")]),
              (mid + 4, [("~ Rustig aan ~", "~ Easy now ~"), ("De eerste", "The first carts"), ("karretjes gaan", "go slowly. Look"),
                         ("langzaam. Kijk!", "at the bar!")]),
              (mid - 4, [("Onderin zie je", "The bar shows"), ("welk erts er nu", "which ore comes"), ("aankomt. Zet de", "next. Set the"),
                         ("wissel vast!", "switch early!")])]
    for i, (x, regels) in enumerate(uitleg):
        assert S.get(x, KAR_Y + 6, 1) in (None, "minecraft:air"), f"Wisselgrot: the sign spot {x} is taken"
        bord(ms, S, x, KAR_Y + 6, 1, "south", regels, texts, f"karretjes.uitleg{i + 2}")
    karretjes_kracht(ms, S, texts, X, mid)
    # (1.7.0 playtest) RAND blocks of room on every side (the world's rock: only the hall is protected) and three extra
    # entrances from the balcony: west, east and north
    R = sh.RAND
    T = sh.verschuif(ms, S, R, 0, R, (X + 2 * R, Y, Z + 2 * R))
    info["lift"] = (lift[0] + R, lift[1], lift[2] + R)
    info["npc"] = (mid + R, KAR_Y + 5, 2 + R)
    info["hal"] = (R, 0, R, R + X - 1, Y - 1, R + Z - 1)
    info["ingangen"] = []
    for (x, z, richting) in ((0, 3, "west"), (X - 1, 3, "east"), (mid + 8, 0, "north")):
        assert S.get(x + (1 if richting == "west" else -1 if richting == "east" else 0), KAR_Y + 5, z + (1 if richting == "north" else 0)) == "minecraft:air"
        kamer = sh.ingang(ms, T, x + R, KAR_Y + 5, z + R, richting, naam=("Wisselgrot", "Switch Cave"), wie=("bij Piepwiel", "Piepwiel's"),
                          texts=texts, prefix=f"karretjes.{richting}", vloer="guhs:bielzenhout", wand="guhs:ertsgruis_bakstenen",
                          plafond=PLAFOND)
        info["ingangen"].append(kamer)
    return T, info


def karretjes_kracht(ms, S, texts, X, mid):
    """The Wisselgrot's vadskracht: a Guhrad at the balcony's east end (wheel block (X - 4, z 1), facing the balcony), a
    lever in front, a Vadsschakelaar; eight Vadslampen on the balcony (where the hanging lanterns were, not by the lift or
    Piepwiel) and two scoreboard lamps flanking Piepwiel; the wire stays in the back half of the balcony (z 1..4: the
    front row is for the players at the levers on the railing): 10 VK."""
    y = KAR_Y + 5
    lampen = [(x, y, 2, "south") for x in range(2, X - 2, 6) if abs(x - mid) > 3]
    return sk.hal(ms, S, "karretjes", texts, "karretjes.kracht", rad=(X - 4, y, 1, "south"), hendel=(X - 4, y, 2, "floor", "south"),
                  schakelaar=(X - 6, y, 1, "west", (X - 7, 1)), lampen=lampen, score=[(mid - 1, y, 1, "south"), (mid + 1, y, 1, "south")],
                  binnen=(1, 1, X - 2, 4), y=y,
                  borden=[(X - 4, y, 3, "south", sk.BORD_RAD, "rad", False), (X - 7, y, 3, "south", sk.BORD_LAMPEN, "lampen", False),
                          (mid - 2, y, 1, "south", sk.BORD_SCORE, "score", False)])


def karretjes_hut(ms, texts):
    """The little station on the surface: 7x7, a sign, the Wolkenliftje in the middle (above the jigsaw)."""
    S = ms.Structure((9, 8, 9))
    c = 4
    for x in range(9):
        for z in range(9):
            S.set(x, 0, z, "guhs:ertsgruissteen")
            for y in range(1, 8):
                S.set(x, y, z, "minecraft:air")
    for x in range(1, 8):
        for z in range(1, 8):
            S.set(x, 1, z, "guhs:bielzenhout")
            wand = x in (1, 7) or z in (1, 7)
            for y in range(2, 5):
                if wand:
                    hoek = x in (1, 7) and z in (1, 7)
                    S.set(x, y, z, "minecraft:stripped_spruce_log" if hoek else ("guhs:ertsgruis_bakstenen" if y == 2 else "guhs:bielzenhout"),
                          {"axis": "y"} if hoek else None)
            S.set(x, 5, z, "guhs:bielzenhout_plaat", {"type": "bottom", "waterlogged": "false"})
    for x in range(0, 9):                                                  # a little roof overhang
        for z in (0, 8):
            S.set(x, 5, z, "guhs:bielzenhout_plaat", {"type": "bottom", "waterlogged": "false"})
    for z in range(0, 9):
        for x in (0, 8):
            S.set(x, 5, z, "guhs:bielzenhout_plaat", {"type": "bottom", "waterlogged": "false"})
    S.set(c, 6, c, "guhs:seinlampje", {"facing": "south"})
    # the door (south side) and windows
    S.set(c, 2, 7, "minecraft:air")
    S.set(c, 3, 7, "minecraft:air")
    for (x, z) in ((1, 4), (7, 4)):
        S.set(x, 3, z, "minecraft:glass_pane", {"north": "true", "south": "true", "east": "false", "west": "false", "waterlogged": "false"})
    S.set(c, 1, c, "guhs:wolkenliftje")
    jigsaw(S, c, 0, c, "down", "guhs:karretjes_hut_onder", "", "minecraft:empty", "guhs:ertsgruissteen")
    S.set(c, 4, c, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    S.set(2, 2, 2, "guhs:ertskarretje", {"facing": "east"})
    S.set(6, 2, 2, "guhs:stationsklokje", {"facing": "south"})
    bord(ms, S, c + 1, 3, 8, "south", [("Wisselgrot", "Switch Cave"), ("Karretjes-wissels", "Cart Switches"), ("Wolkenliftje", "Cloud lift"),
                                      ("sluipen = omlaag", "sneak = down")], texts, "karretjes.hut")
    bord(ms, S, c, 3, 2, "south", [("Op het", "Stand on the"), ("wolkenliftje", "cloud lift"), ("sluipen:", "and sneak:"),
                                  ("naar beneden!", "down you go!")], texts, "karretjes.lift")
    for x in (1, 2, 3, 5, 6, 7):                                           # a little fence in front (connected: sh.verbind)
        S.set(x, 2, 8, "minecraft:spruce_fence")
    return sh.hut(ms, S, vloer_y=1, fundering="guhs:ertsgruissteen")


# =====================================================================================================================
# the Kegelgrot
# =====================================================================================================================
def kegel_midden(g, u):
    if u <= g["BOCHT_VAN"] or u >= g["BOCHT_TOT"]:
        return 0.0
    return g["BOCHT"] * math.sin(math.pi * (u - g["BOCHT_VAN"]) / (g["BOCHT_TOT"] - g["BOCHT_VAN"]))


KEG_BANEN = [("recht", -3, 3), ("bochtig", -3, 4), ("hindernis", -3, 3)]   # (soort, v min, v max of the lane's strip)


def kegelgrot_zaal(ms, texts, rng):
    g = kegel_getallen()
    xs, banen = 1, []
    for soort, vmin, vmax in KEG_BANEN:
        w = vmax - vmin + 1
        banen.append((soort, xs, w, xs + vmax, vmin, vmax))
        xs += w
    X, Z, Y = xs + 1, KEG_Z0 + KEG_LENGTE + 3, KEG_HOOG + 1
    S = ms.Structure((X, Y, Z))
    doos(S, 0, 0, 0, X - 1, KEG_HOOG, Z - 1, "guhs:viltsteen_bakstenen", "guhs:retrotegel", "guhs:gepolijste_viltsteen")
    for x in range(X):
        for z in range(Z):
            S.set(x, 0, z, "guhs:viltsteen")
            S.set(x, KEG_Y, z, "guhs:retrotegel" if (x + z) % 2 == 0 else "guhs:retrotegel_paars")
    # the walls: pink wool panels with neon signs over the pins
    for x in range(X):
        for y in range(KEG_Y + 1, KEG_HOOG):
            S.set(x, y, Z - 1, "minecraft:black_wool")                 # the curtain behind the pins
        for z in range(Z):
            for y in range(KEG_Y + 1, KEG_HOOG):
                if x in (0, X - 1) and y in (KEG_Y + 3,):
                    S.set(x, y, z, "minecraft:pink_concrete")
    info = {"banen": [], "lift": None, "npc": None}
    for b, (soort, x0, w, mx, vmin, vmax) in enumerate(banen):
        for u in range(-7, KEG_LENGTE + 2):
            for v in range(vmin, vmax + 1):
                x, z = mx - v, KEG_Z0 + u
                if u < 0:
                    S.set(x, KEG_Y, z, "guhs:kegelbaanhout")                  # the approach
                    continue
                if u > KEG_LENGTE:                                            # the pit
                    S.set(x, KEG_Y - 1, z, "minecraft:black_wool")
                    S.set(x, KEG_Y, z, "minecraft:air")
                    continue
                c = kegel_midden(g, u) if soort == "bochtig" else 0.0
                d = abs(v - c)
                if soort == "recht":
                    if abs(v) <= 1:
                        S.set(x, KEG_Y, z, "guhs:kegelbaanhout")
                    elif abs(v) == 2:
                        S.set(x, KEG_Y, z, "guhs:kegelgrot_goot")
                    else:
                        S.set(x, KEG_Y, z, "guhs:kegelbaanhout")
                        S.set(x, KEG_Y + 1, z, "guhs:kegelgrot_bumper")       # (the low wall between the lanes)
                else:
                    S.set(x, KEG_Y, z, "guhs:kegelbaanhout")
                    if d > g["HALF"] + 0.001 and d <= g["HALF"] + 1.0:
                        S.set(x, KEG_Y + 1, z, "guhs:kegelgrot_bumper")
                    elif d > g["HALF"] + 1.0:
                        S.set(x, KEG_Y + 1, z, "guhs:kegelgrot_bumper" if v in (vmin, vmax) else "minecraft:air")
        if soort == "hindernis":
            for v in (-int(g["PAAL_V"]), int(g["PAAL_V"])):
                S.set(mx - v, KEG_Y + 1, KEG_Z0 + int(g["PAAL_U"]), "guhs:kegelgrot_paaltje")
        S.set(mx, KEG_Y, KEG_Z0, "guhs:kegelgrot_baanbegin", {"facing": "south", "baan": str(b)})
        for v in (-1, 1):
            S.set(mx - v, KEG_Y, KEG_Z0, "guhs:kegelbaanhout")
        # a neon sign above the pins, lamps above the lane
        S.set(mx, KEG_Y + 4, Z - 2, "guhs:neon_kegelbord", {"facing": "north"})
        for u in (2, 7):                                              # (u 12: the Kegelzetter's lamps light the pins now)
            S.set(mx, KEG_HOOG - 1, KEG_Z0 + u, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
        naam = {"recht": [("~ Baan 1 ~", "~ Lane 1 ~"), ("Recht", "Straight"), ("Makkelijk", "Easy"), ("Pas op: goten!", "Mind the gutters!")],
                "bochtig": [("~ Baan 2 ~", "~ Lane 2 ~"), ("Bochtig", "Bendy"), ("Medium", "Medium"), ("Kronkel, guhtje!", "Wiggle, guhtje!")],
                "hindernis": [("~ Baan 3 ~", "~ Lane 3 ~"), ("Hindernisjes", "Obstacles"), ("Lastig!", "Hard!"), ("Pas op de Mika!", "Mind the Mika!")]}
        S.set(mx, KEG_Y + 1, KEG_Z0 - 8, "minecraft:cherry_fence", {"east": "false", "west": "false", "north": "false", "south": "false",
                                                                    "waterlogged": "false"})
        bord(ms, S, mx, KEG_Y + 2, KEG_Z0 - 8, 0, naam[soort], texts, f"kegelen.baan_{soort}", wall=False, hout="cherry")
        info["banen"].append({"mx": mx, "soort": soort})
    # the lounge (z 1..4): De Guhde behind his counter, the Wolkenliftje, benches, a ball rack
    mid = X // 2
    lift = (3, KEG_Y, 2)
    S.set(*lift, "guhs:wolkenliftje")
    jigsaw(S, lift[0], KEG_HOOG, lift[2], "up", "guhs:kegelgrot_lift", "guhs:kegelgrot_schacht_onder", "guhs:kegelgrot/schacht",
           "guhs:gepolijste_viltsteen")
    info["lift"] = lift
    for x in range(mid - 2, mid + 3):
        S.set(x, KEG_Y + 1, 3, "guhs:kegelbaanhout_plaat", {"type": "top", "waterlogged": "false"})   # the counter
    npc(ms, S, mid, KEG_Y + 1, 2, "kegelguh", "south")
    info["npc"] = (mid, KEG_Y + 1, 2)
    S.set(mid - 2, KEG_Y + 2, 3, "guhs:kegeltje", {"facing": "south"})
    S.set(mid + 2, KEG_Y + 2, 3, "guhs:kegeltje", {"facing": "south"})
    S.set(mid + 4, KEG_Y + 1, 1, "guhs:wolbalrek", {"facing": "south"})
    S.set(mid - 4, KEG_Y + 1, 1, "guhs:wolbalrek", {"facing": "south"})
    for x in range(X - 6, X - 2):                                         # a bench to watch from
        S.set(x, KEG_Y + 1, 1, "guhs:kegelbaanhout_trap", {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})
    S.set(mid, KEG_Y + 4, 1, "guhs:neon_kegelbord", {"facing": "south"})
    bord(ms, S, mid - 1, KEG_Y + 3, 1, "south", [("De Kegelgrot", "The Bowling Cave"), ("Kijk, hou vast,", "Look, hold on,"),
                                                 ("laat los: rol!", "let go: roll!"), ("Strike = VAHOEG", "Strike = WAHOOG")],
         texts, "kegelen.uitleg")
    # (ronde 45-47) the lounge's lamps are Vadslampen on a Guhrad behind De Guhde, and the Kegelzetter over the pins
    info["zetter"] = kegelgrot_kracht(ms, S, texts, X, mid, banen)
    # (1.7.0 playtest) RAND blocks of room on every side and three extra entrances from the lounge: west, east and north
    R = sh.RAND
    T = sh.verschuif(ms, S, R, 0, R, (X + 2 * R, Y, Z + 2 * R))
    info["lift"] = (lift[0] + R, lift[1], lift[2] + R)
    info["npc"] = (mid + R, KEG_Y + 1, 2 + R)
    info["hal"] = (R, 0, R, R + X - 1, Y - 1, R + Z - 1)
    info["ingangen"] = []
    for (x, z, richting) in ((0, 2, "west"), (X - 1, 2, "east"), (6, 0, "north")):
        assert S.get(x + (1 if richting == "west" else -1 if richting == "east" else 0), KEG_Y + 1, z + (1 if richting == "north" else 0)) in (None, "minecraft:air")
        kamer = sh.ingang(ms, T, x + R, KEG_Y + 1, z + R, richting, naam=("Kegelgrot", "Bowling Cave"), wie=("bij De Guhde", "The Guhde's"),
                          texts=texts, prefix=f"kegelen.{richting}", vloer="guhs:kegelbaanhout", wand="guhs:viltsteen_bakstenen",
                          plafond="guhs:gepolijste_viltsteen", deur="minecraft:cherry_door", hout="cherry")
        info["ingangen"].append(kamer)
    return T, info


# the Kegelzetter (Java mijnspellen/kegelgrot/Kegelzetter.java, in a lane's frame from its foul line: u along the lane, v to
# its right, dy above the foul line block): the bridge at u ZETTER_U (dy 3), the lane's Vadslamp on it (dy 4, v -1: the
# neon sign behind stays in sight), the lane's sign on the bridge's front (u ZETTER_BORD_U, dy 3, v 0)
ZETTER_U, ZETTER_BORD_U, ZETTER_DY = 15, 14, 3
ZETTER_AAN = [("Kegelzetter", "Pin setter"), ("op vadskracht!", "on vads power!"), ("Zet je kegels", "Sets your pins"), ("klaar. Strike!", "up. Strike!")]
ZETTER_UIT = [("Kegelzetter", "Pin setter"), ("heeft PAUZE!", "takes a BREAK!"), ("De Guhde zet ze", "The Guhde sets"), ("met de hand.", "them by hand.")]


def kegelgrot_kracht(ms, S, texts, X, mid, banen):
    """The Kegelgrot's vadskracht, two Guhraden:
      - the lounge: a Guhrad right behind De Guhde (wheel block (mid + 2, z 1)), its lever beside him, a Vadsschakelaar; two
        scoreboard lamps on his counter (wired over the counter top), four Vadslampen along the front of the lounge: 6 VK;
      - the Kegelzetter: a bridge of kegelbaanhout over the end of the lanes, a Guhrad on its west end (its lever on the
        bridge's front: it powers the bridge block under the wheel), a Vadsschakelaar and a Vadslamp per lane on the
        bridge, wired along its top: 3 VK; and per lane the Kegelzetter sign (Java swaps it to "pauze" without power).
    Returns the setter's spots (for the check against the Java)."""
    y = KEG_Y + 1
    lounge = sk.hal(ms, S, "kegelgrot", texts, "kegelen.kracht", rad=(mid + 2, y, 1, "south"), hendel=(mid + 2, y, 2, "floor", "south"),
                    schakelaar=(mid, y, 1, "west", (mid, 2)), lampen=[(6, y, 4, "south"), (9, y, 4, "south"), (X - 7, y, 4, "south"), (X - 2, y, 4, "south")],
                    score=[(mid - 1, y + 1, 3, "south"), (mid + 1, y + 1, 3, "south")], binnen=(1, 1, X - 2, 5), y=y,
                    borden=[(mid + 5, y, 2, "south", sk.BORD_RAD, "rad", False), (mid + 4, y, 2, "south", sk.BORD_LAMPEN, "lampen", False),
                            (mid - 2, y, 2, "south", sk.BORD_SCORE, "score", False)])
    S.set(mid, y + 1, 3, "guhs:guh_wire")      # (on the counter between the two scoreboard lamps, a step up from the wire at (mid, 2))
    # the bridge (u ZETTER_U, a block above the pins' heads) over the whole width
    zb = KEG_Z0 + ZETTER_U
    by = KEG_Y + ZETTER_DY
    for x in range(1, X - 1):
        S.set(x, by, zb, "guhs:kegelbaanhout")
    lampen = [(mx + 1, by + 1, zb, "north") for (_s, _x0, _w, mx, _vmin, _vmax) in banen]
    assert lampen[0][0] == 5, lampen
    zetter = sk.hal(ms, S, "kegelzetter", texts, "kegelen.kracht_zetter", rad=(2, by + 1, zb, "north"), hendel=(2, by, zb - 1, "wall", "north"),
                    schakelaar=(4, by + 1, zb, "north", None), lampen=lampen, binnen=(0, 0, 0, 0), y=by + 1, draad=False,
                    borden=[(1, by, zb - 1, "north", sk.BORD_RAD, "rad", True)])
    for x in range(lampen[0][0] + 1, lampen[-1][0]):                      # the wire along the bridge, lamp to lamp
        if S.get(x, by + 1, zb) in (None, "minecraft:air"):
            S.set(x, by + 1, zb, "guhs:guh_wire")
    for b, (_s, _x0, _w, mx, _vmin, _vmax) in enumerate(banen):
        bord(ms, S, mx, by, KEG_Z0 + ZETTER_BORD_U, "north", ZETTER_AAN, texts, "kegelen.zetter_aan")
    for i, (nl, en) in enumerate(ZETTER_UIT):
        texts[f"sign.guhs.kegelen.zetter_uit_{i}"] = (en, nl)
    return {"lounge": lounge, "zetter": zetter}


def kegelgrot_hut(ms, texts):
    """A little round-ish hut with a giant plush pin on its roof."""
    S = ms.Structure((9, 11, 9))
    c = 4
    for x in range(9):
        for z in range(9):
            S.set(x, 0, z, "guhs:viltsteen")
            for y in range(1, 11):
                S.set(x, y, z, "minecraft:air")
    for x in range(1, 8):
        for z in range(1, 8):
            if (x in (1, 7)) and (z in (1, 7)):
                continue
            S.set(x, 1, z, "guhs:retrotegel" if (x + z) % 2 == 0 else "guhs:retrotegel_paars")
            wand = x in (1, 7) or z in (1, 7)
            for y in range(2, 5):
                if wand:
                    S.set(x, y, z, "minecraft:pink_concrete" if y != 3 else "minecraft:white_concrete")
            S.set(x, 5, z, "guhs:kegelbaanhout_plaat", {"type": "bottom", "waterlogged": "false"})
    # the giant pin on the roof: white wool with a pink stripe and a guh head
    for y, r, blok in ((6, 1, "minecraft:white_wool"), (7, 1, "minecraft:white_wool"), (8, 0, "minecraft:pink_wool"), (9, 1, "minecraft:white_wool")):
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                S.set(c + dx, y, c + dz, blok)
    S.set(c - 1, 10, c, "minecraft:pink_wool")                           # the guh ears
    S.set(c + 1, 10, c, "minecraft:pink_wool")
    S.set(c, 9, c + 1, "minecraft:pink_concrete")                         # a little nose
    S.set(c, 2, 7, "minecraft:air")
    S.set(c, 3, 7, "minecraft:air")
    S.set(c, 1, c, "guhs:wolkenliftje")
    jigsaw(S, c, 0, c, "down", "guhs:kegelgrot_hut_onder", "", "minecraft:empty", "guhs:viltsteen")
    S.set(c, 4, c, "minecraft:lantern", {"hanging": "true", "waterlogged": "false"})
    S.set(2, 2, 2, "guhs:kegeltje", {"facing": "south"})
    S.set(6, 2, 2, "guhs:wolbalrek", {"facing": "south"})
    bord(ms, S, c + 1, 3, 8, "south", [("De Kegelgrot", "The Bowling Cave"), ("Guh-bowlen", "Guh bowling"), ("bij De Guhde", "with The Guhde"),
                                      ("Sluip op 't liftje", "Sneak on the lift")], texts, "kegelen.hut")
    for x in (2, 3, 5, 6):                                                 # pink guh fences in front (connected: sh.verbind)
        S.set(x, 2, 8, "guhs:guh_hekje_roze")
    return sh.hut(ms, S, vloer_y=1, fundering="guhs:viltsteen")


# =====================================================================================================================
# the shafts, the test floor, the worldgen files
# =====================================================================================================================
def test_vloer(ms):
    """GameTests: a stone floor (y 0) of 28 x 22 with room above."""
    S = ms.Structure((28, 10, 22))
    for x in range(28):
        for z in range(22):
            S.set(x, 0, z, "minecraft:stone")
    return S


STRUCTUREN = {"karretjes": "karretjeswissels", "kegelgrot": "kegelgrot"}
BIOMES = ["guhs:belletjesgrot", "guhs:beverbeek", "guhs:bleekwoud", "guhs:bloemenvallei", "guhs:bloesemmeertje", "guhs:glitterspelonk",
          "guhs:guh_fields", "guhs:guh_meadows", "guhs:guh_peaks", "guhs:guhpolder", "guhs:kaas_flats", "guhs:kaasmoeras", "guhs:klaterdal",
          "guhs:knabbel_crumbs", "guhs:knuffeldal", "guhs:knuffelwortelgrot", "guhs:kristalpegelgrot", "guhs:pink_puffs", "guhs:pluisgrot",
          "guhs:slaapgrot", "guhs:sneeuwguhtoendra", "guhs:vads_cliffs", "guhs:vadswoud"]
SALT = {"karretjes": 17072201, "kegelgrot": 17072202}


def build(h, ms, texts):
    import random
    problems = check_java()
    if problems:
        raise AssertionError("mijnspellen: the networks differ from KarretjesNet.java:\n  " + "\n  ".join(problems))
    rng = random.Random(1707)
    zaal, kinfo = karretjes_zaal(ms, texts, rng)
    kzaal, ginfo = kegelgrot_zaal(ms, texts, rng)
    problems = []
    for spel, z, info, hut in (("karretjes", zaal, kinfo, karretjes_hut(ms, texts)), ("kegelgrot", kzaal, ginfo, kegelgrot_hut(ms, texts))):
        sid = STRUCTUREN[spel]
        sch = sh.schacht(ms, spel, sid, SCHACHT)
        sk.verbind(z)
        for naam, t in ((f"{sid}_zaal", z), (f"{sid}_hut", hut)):
            sh.verbind(t, h.A)
            problems += sh.check_verbonden(t, h.A, naam)
        problems += sh.check_gebouw(spel, z, info, hut, h.A)
        z.save(f"{sid}_zaal")
        hut.save(f"{sid}_hut")
        sch.save(f"{sid}_schacht")
        sh.worldgen(h, sid, spel, templates={"start": f"{sid}_zaal", "schacht": f"{sid}_schacht", "hut": f"{sid}_hut"},
                    hal_hoogte=z.size[1], schacht_hoogte=SCHACHT, biomes=BIOMES, salt=SALT[spel])
    test_vloer(ms).save("mijnspellen_test")
    if problems:
        raise AssertionError("mijnspellen self-check:\n  " + "\n  ".join(problems))
    return kinfo, ginfo
