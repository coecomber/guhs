"""Zwamzoet as a map stamp: zwamzoet.bouw(S) with world tweaks, written with its offset + POIs to an .npz.

python stad_wereld.py <out.npz>
World = scene + (OX, OY, OZ): scene (128, 128) -> world (0, 0); ground G=8 -> y 63 (just above the bay at 62).
Tweaks against the concept render:
  - terrain: only the grass layer (the tile writer makes the ground below; the bay is the global sea);
  - harbour: no airship mast (the airship docks at De Haven, contract area `haven`);
  - lifts: real wolkenlift columns (up + down) instead of a chain;
  - the 10 ground houses -> shops with interiors (smidse, veilinghuis, bank, uitrusting, kruidenier, kleermaker,
    havenmeester) + 6 citizens' houses; questbord, campfire, Mika clues; the ordetempel gets an interior + side door.
"""
import json
import math
import sys

import numpy as np

import stadkit as SK
import zwamzoet as Z

OX, OY, OZ = -128, 55, -128
G = Z.G
POI = {}
DEUREN = {}
GEBOUWEN = {}


def w(x, y, z):
    return [int(x + OX), int(y + OY), int(z + OZ)]


# ------------------------------------------------------------------------------------------------ patches
def terrein(S):
    X, Y, Z_ = S.v.shape
    for x in range(X):
        kust = 216 + int(3 * math.sin(x * 0.05))
        S.v[x, G, :kust] = S.id("knuffelgras")
        if 30 < x < 236:
            for z in range(208, kust + 1):
                for y in range(1, G + 1):
                    S.set(x, y, z, "knuffelsteen" if y < G else Z.KLINK)
            S.set(x, G + 1, kust, Z.HEK if x % 6 else Z.LANT)


def haven(S):
    for px in (84, 160):
        for z in range(214, 246):
            S.fill(px - 1, G, z, px + 1, G, z, Z.PLANK)
            if z % 4 == 0:
                for dx in (-2, 2):
                    for y in range(1, G + 1):
                        S.set(px + dx, y, z, "minecraft:cherry_log[axis=y]")
            if z % 8 == 0:
                S.set(px + 2, G + 1, z, Z.LANT)
    for (bx, bz) in ((92, 228), (152, 236), (70, 240)):
        Z.bootje(S, bx, bz)
    Z.vuurtoren(S, 34, 234)
    zwemtrappen(S)


ZEE_LOKAAL = 62 - OY          # the global sea's water top (kaart.ZEE) in scene y


def zwemtrap(S, x, z, uit, y_top=G, y_bodem=59 - OY, steun="minecraft:cherry_log[axis=y]"):
    """Scene-coordinate twin of bouwsels.zwemtrap: a ladder out of the bay in column (x, z) up to the deck/quay floor
    y_top, hanging on the support column (x, z) - uit (filled solid below the deck), headroom carved above."""
    f = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[uit]
    sx, sz = x - uit[0], z - uit[1]
    for y in range(y_bodem, y_top):
        if S.get(sx, y, sz) in ("minecraft:air", "minecraft:cave_air"):
            S.set(sx, y, sz, steun)
    for y in range(1, y_bodem):                  # solid under the ladder and its post (the sea floor is only a skin over the hollow)
        S.set(x, y, z, "minecraft:sandstone")
        S.set(sx, y, sz, "minecraft:sandstone")
    for y in range(y_bodem, y_top + 1):
        S.set(x, y, z, f"minecraft:ladder[facing={f},waterlogged={'true' if y <= ZEE_LOKAAL else 'false'}]")
    for y in (y_top + 1, y_top + 2):
        S.set(x, y, z, "minecraft:cave_air")
        S.set(sx, y, sz, "minecraft:cave_air")


def zwemtrappen(S):
    """Ways out of the bay (fix round 1): ladders at the end and on both sides of the two jetties, and every ~14 blocks
    up the fenced quay (the fence/lantern above the ladder is cleared)."""
    for px in (84, 160):
        zwemtrap(S, px, 246, (0, 1))
        for z in (222, 234):
            zwemtrap(S, px - 2, z + 1, (-1, 0))
            zwemtrap(S, px + 2, z + 1, (1, 0))
    for x in range(38, 236, 14):
        if any(abs(x - px) <= 4 for px in (84, 160)) or abs(x - 34) <= 9:
            continue
        kust = 216 + int(3 * math.sin(x * 0.05))
        zwemtrap(S, x, kust + 1, (0, 1))


def lift(S, naam, hoek, zw):
    """Wolkenlift: open shaft with an up column (wolkenlift + wolkenstroom) and a down column next to it.
    Top: a roofed station flush with the cap, the puff drops you on it facing the plateau, and a stair ramp (3 wide,
    1 block per step, headroom kept free) climbs from the station up to the plateau floor. Bottom: a paved square
    with a path to the nearest town path."""
    cx, cz, T, r, rs, _, p, _ = zw[naam]
    a = math.radians(hoek)
    lx, lz = int(round(cx + (r * 0.84) * math.cos(a))), int(round(cz + (r * 0.84) * math.sin(a)))
    d = math.hypot(lx - cx, lz - cz)
    top, _ = Z.hoed_profiel(d, T, r, p)
    vx, vz = cx - lx, cz - lz
    naar = ("east" if vx > 0 else "west") if abs(vx) > abs(vz) else ("south" if vz > 0 else "north")
    sx, sz = {"east": (1, 0), "west": (-1, 0), "south": (0, 1), "north": (0, -1)}[naar]
    ox, oz = sz, sx
    for y in range(G + 1, top + 4):
        if y < top:
            for (dx, dz) in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
                S.set(lx + dx, y, lz + dz, Z.HEK if y % 12 else "roze_lakhout_planken")
        for (dx, dz) in ((0, -1), (0, 1), (-1, 0), (1, 0), (0, 0)):
            S.set(lx + dx, y, lz + dz, "minecraft:air")
    S.set(lx, G + 1, lz, f"wolkenlift[down=false,facing={naar}]")
    S.set(lx + ox, G + 1, lz + oz, f"wolkenlift[down=true,facing={naar}]")
    for y in range(G + 2, top + 3):
        S.set(lx, y, lz, f"wolkenstroom[down=false,facing={naar}]")
        S.set(lx + ox, y, lz + oz, f"wolkenstroom[down=true,facing={naar}]")
    # station: plank floor at the cap surface with the two columns through it, free headroom, posts on the corners
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if (dx, dz) not in ((0, 0), (ox, oz)):
                S.set(lx + dx, top, lz + dz, Z.PLANK)
                for hy in (1, 2, 3, 4):
                    S.set(lx + dx, top + hy, lz + dz, "minecraft:air")
    for (dx, dz) in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        for hy in (1, 2, 3):
            S.set(lx + dx, top + hy, lz + dz, Z.HEK)
        S.set(lx + dx, top + 4, lz + dz, Z.LANT_H)
    S.fill(lx - 2, top + 5, lz - 2, lx + 2, top + 5, lz + 2, "guh_dakpan_roze")
    # ramp from the station (exit side = naar) up to the plateau
    y, k = top, 3
    while True:
        bx, bz = lx + sx * k, lz + sz * k
        dd = math.hypot(bx - cx, bz - cz)
        if y < T:
            y += 1
            blok = SK.stairs("minecraft:cherry_stairs", naar)
        else:
            blok = Z.PLANK
        for w_ in (-1, 0, 1):
            x_, z_ = bx + ox * w_, bz + oz * w_
            S.set(x_, y, z_, blok)
            if blok != Z.PLANK:
                S.set(x_, y - 1, z_, Z.PLANK)
            if blok != Z.PLANK:          # headroom over the steps (the cap is in the way); the plateau itself is left alone
                for hy in (1, 2, 3):
                    if not S.get(x_, y + hy, z_).startswith(("guhs:knuffelsteen", "guhs:tegel_", "guhs:pastel")):
                        S.set(x_, y + hy, z_, "minecraft:air")
        if y >= T and (blok == Z.PLANK or dd < p * r - 2):
            break
        k += 1
    # bottom: paved square + a path to the nearest town path
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            if S.get(lx + dx, G, lz + dz) in ("guhs:knuffelgras", "guhs:knuffelklinkers", "guhs:tegel_hartjes"):
                S.set(lx + dx, G, lz + dz, "tegel_hartjes" if max(abs(dx), abs(dz)) == 3 else Z.KLINK)
    beste = None
    for (x1, z1, x2, z2, b) in Z.PADEN:
        L = math.hypot(x2 - x1, z2 - z1) or 1
        t_ = max(0.0, min(1.0, ((lx - x1) * (x2 - x1) + (lz - z1) * (z2 - z1)) / (L * L)))
        qx, qz = x1 + (x2 - x1) * t_, z1 + (z2 - z1) * t_
        afst = math.hypot(qx - lx, qz - lz)
        if beste is None or afst < beste[0]:
            beste = (afst, qx, qz)
    if beste and beste[0] > 3:
        Z.pad(S, lx, lz, beste[1], beste[2], 1)
    POI[f"lift_{naam}_voet"] = w(lx, G + 1, lz)
    POI[f"lift_{naam}_top"] = w(lx + sx * 2, top + 1, lz + sz * 2)


# ------------------------------------------------------------------------------------------------ shops
def meubel(S, soort, x0, z0, x1, z1, y, voor):
    """Interior per shop (floor y = first air layer). x0..x1/z0..z1 = the inside."""
    mx, mz = (x0 + x1) // 2, (z0 + z1) // 2
    # lights in every shop
    for (lx, lz) in ((x0 + 1, z0 + 1), (x1 - 1, z0 + 1), (x0 + 1, z1 - 1), (x1 - 1, z1 - 1)):
        S.set(lx, y, lz, "minecraft:lantern[hanging=false,waterlogged=false]")
    # the counter runs across the back half, opposite the door
    if voor in ("east", "west"):
        cx_ = x0 + 2 if voor == "east" else x1 - 2
        for z in range(z0 + 1, z1):
            if z != mz:
                S.set(cx_, y, z, "minecraft:cherry_slab[type=top,waterlogged=false]" if soort != "bank" else "minecraft:gold_block")
        achter = [(cx_ + (-1 if voor == "east" else 1), y, z) for z in range(z0, z1 + 1)]
    else:
        cz_ = z0 + 2 if voor == "south" else z1 - 2
        for x in range(x0 + 1, x1):
            if x != mx:
                S.set(x, y, cz_, "minecraft:cherry_slab[type=top,waterlogged=false]" if soort != "bank" else "minecraft:gold_block")
        achter = [(x, y, cz_ + (-1 if voor == "south" else 1)) for x in range(x0, x1 + 1)]
    spul = {
        "smidse": ["minecraft:anvil[facing=north]", "minecraft:blast_furnace[facing=south,lit=false]", "minecraft:smithing_table",
                   "minecraft:grindstone[face=floor,facing=north]", "minecraft:barrel[facing=up,open=false]"],
        "veilinghuis": ["minecraft:lectern[facing=south,has_book=false,powered=false]", "minecraft:barrel[facing=up,open=false]",
                        "minecraft:chest[facing=east,type=single,waterlogged=false]", "minecraft:cartography_table"],
        "bank": ["minecraft:chest[facing=east,type=single,waterlogged=false]", "minecraft:gold_block", "minecraft:iron_block",
                 "minecraft:barrel[facing=up,open=false]"],
        "uitrusting": ["minecraft:smithing_table", "minecraft:barrel[facing=up,open=false]", "minecraft:fletching_table",
                       "minecraft:target"],
        "kruidenier": ["minecraft:brewing_stand[has_bottle_0=false,has_bottle_1=false,has_bottle_2=false]",
                       "minecraft:cauldron", "minecraft:barrel[facing=up,open=false]", "minecraft:composter[level=0]"],
        "kleermaker": ["minecraft:loom[facing=north]", "minecraft:pink_wool", "minecraft:white_wool", "minecraft:light_blue_wool",
                       "minecraft:barrel[facing=up,open=false]"],
        "havenmeester": ["minecraft:cartography_table", "minecraft:barrel[facing=up,open=false]",
                         "minecraft:chest[facing=east,type=single,waterlogged=false]"],
        "huis": ["minecraft:red_bed[facing=north,occupied=false,part=foot]", "minecraft:barrel[facing=up,open=false]",
                 "minecraft:flower_pot"],
    }[soort]
    for i, p in enumerate(achter[1:-1]):
        if i % 2 == 0:
            S.set(*p, spul[(i // 2) % len(spul)])
    if soort == "bank":
        for p in achter[1:-1]:
            S.set(p[0], p[1] + 1, p[2], "minecraft:iron_bars")
    return (mx, y, mz)


def winkel(S, soort, x0, z0, wd, dp, voor, h=6, pal=0):
    top = Z.huis(S, x0, G + 1, z0, wd, dp, h, voor, Z.PALETTEN[pal])
    x1, z1 = x0 + wd - 1, z0 + dp - 1
    mid = (x0 + x1) // 2 if voor in ("north", "south") else (z0 + z1) // 2
    deur = {"south": (mid, G + 1, z1 + 1), "north": (mid, G + 1, z0 - 1),
            "east": (x1 + 1, G + 1, mid), "west": (x0 - 1, G + 1, mid)}[voor]
    binnen = meubel(S, soort, x0 + 1, z0 + 1, x1 - 1, z1 - 1, G + 1, voor) if soort else None
    GEBOUWEN.setdefault(soort or "huis", []).append(dict(deur=w(*deur), binnen=w(*binnen) if binnen else None,
                                                        doos=[w(x0, G, z0), w(x1, top, z1)]))
    return deur, binnen


def grondhuisjes(S, rng):
    winkel(S, "smidse", 78, 171, 11, 11, "east", 7, 3)
    winkel(S, "veilinghuis", 162, 168, 15, 15, "west", 8, 1)
    winkel(S, "bank", 148, 136, 11, 9, "south", 6, 2)
    winkel(S, "uitrusting", 98, 136, 11, 9, "south", 6, 0)
    winkel(S, "kruidenier", 82, 192, 11, 9, "east", 6, 4)
    winkel(S, "kleermaker", 164, 192, 11, 9, "west", 6, 1)
    winkel(S, "havenmeester", 56, 196, 9, 9, "south", 5, 2)
    for i, (x, z, v) in enumerate(((36, 176, "east"), (212, 176, "west"), (60, 118, "south"), (188, 116, "south"),
                                    (106, 56, "south"), (142, 56, "south"))):
        if Z.vrij(S, x - 1, z - 1, x + 8, z + 8):
            deur, binnen = winkel(S, "huis", x, z, 7, 7, v, 5, i % 5)


# ------------------------------------------------------------------------------------------------ extras after bouw
def tuintjes(S, rng):
    """Little flower strips OUTSIDE along the side and back walls of the ground buildings (never at the door side)."""
    bloem = ["minecraft:pink_petals[facing=north,flower_amount=4]", "roze_guhbloem", "minecraft:pink_tulip",
             "minecraft:allium", "minecraft:azure_bluet", "minecraft:lily_of_the_valley"]
    for soort, lijst in GEBOUWEN.items():
        for g in lijst:
            (x0, y0, z0), (x1, _, z1) = g["doos"]
            if y0 != G + OY:
                continue
            x0, z0, x1, z1 = x0 - OX, z0 - OZ, x1 - OX, z1 - OZ
            dx_, _, dz_ = g["deur"]
            dx_, dz_ = dx_ - OX, dz_ - OZ
            rand = [(x, z0 - 1) for x in range(x0, x1 + 1)] + [(x, z1 + 1) for x in range(x0, x1 + 1)] +                    [(x0 - 1, z) for z in range(z0, z1 + 1)] + [(x1 + 1, z) for z in range(z0, z1 + 1)]
            for (x, z) in rand:
                if abs(x - dx_) <= 2 and abs(z - dz_) <= 2 or (x == dx_ or z == dz_) and abs(x - dx_) + abs(z - dz_) <= 3:
                    continue
                if (x == dx_ and abs(z - dz_) < 6) or (z == dz_ and abs(x - dx_) < 6):
                    continue
                if S.get(x, G, z) == "guhs:knuffelgras" and S.is_air(x, G + 1, z) and rng.random() < 0.8:
                    S.set(x, G + 1, z, rng.choice(bloem))


def terrein_weg(S):
    """The map terrain around the city is at y 64-65, one or two above the scene's ground (63): without this the
    tile writer's grass covered the plaza, the paths and the floors inside the houses. Every air cell in the three
    layers above the scene's ground becomes cave_air (= carve: clears the terrain, leaves air)."""
    ca = S.id("minecraft:cave_air")
    grond = S.v[:, G, :] > 0
    for y in range(G + 1, G + 4):
        laag = S.v[:, y, :]
        laag[grond & (laag == 0)] = ca



def questbord(S, x, z):
    for k in range(4):
        S.set(x + k, G + 1, z, "minecraft:spruce_fence")
    S.fill(x, G + 2, z, x + 3, G + 4, z, "minecraft:spruce_planks")
    S.fill(x, G + 5, z, x + 3, G + 5, z, SK.stairs("guhs:guh_dakpan_roze_trap", "north"))
    S.set(x - 1, G + 5, z, "minecraft:lantern[hanging=false,waterlogged=false]")
    S.set(x + 4, G + 5, z, "minecraft:lantern[hanging=false,waterlogged=false]")
    S.fill(x, G + 2, z + 1, x + 3, G + 4, z + 1, "minecraft:air")


def ordetempel_binnen(S, info):
    cx, cz, T, r, _, _, p, _ = Z.ZWAMMEN["tempel"]
    yv = T + 1 + 3
    hx1, hx2, hz1, hz2 = cx - 13, cx + 13, cz - 11, cz + 8
    # lights + carpet + the class altar with three pedestals and a statue
    for x in range(hx1 + 3, hx2 - 2, 5):
        for z in range(hz1 + 3, hz2 - 1, 5):
            S.set(x, yv + 23, z, "minecraft:shroomlight")
            S.set(x, yv + 22, z, "minecraft:iron_chain[axis=y]")
            S.set(x, yv + 21, z, "minecraft:lantern[hanging=true,waterlogged=false]")
    # floor: pink/white marble look (quartz + pink concrete in a 2x2 checker), aisle in smooth quartz with a pink line
    for x in range(hx1 + 1, hx2):
        for z in range(hz1 + 1, hz2 + 1):
            S.set(x, yv - 1, z, "minecraft:quartz_block" if ((x - cx) // 2 + (z - cz) // 2) % 2 else "minecraft:pink_concrete")
    for x in range(hx1 + 3, hx2 - 1, 6):           # glowing floor tiles + wall lights at eye height
        for z in range(hz1 + 3, hz2 - 1, 6):
            S.set(x, yv - 1, z, "minecraft:pearlescent_froglight[axis=y]")
    for z in range(hz1 + 2, hz2 - 1, 4):
        S.set(hx1 + 1, yv + 3, z, "minecraft:shroomlight")
        S.set(hx2 - 1, yv + 3, z, "minecraft:shroomlight")
    for z in range(hz1 + 1, hz2 + 1):
        for x in (cx - 1, cx, cx + 1):
            S.set(x, yv - 1, z, "minecraft:pink_concrete" if x == cx else "minecraft:smooth_quartz")
    for lx, kl, ding in ((-6, "minecraft:red_wool", "minecraft:anvil[facing=north]"),
                         (0, "minecraft:lime_wool", "minecraft:target"),
                         (6, "minecraft:purple_wool", "minecraft:enchanting_table")):
        S.set(cx + lx, yv, hz1 + 4, "roze_knuffelsteen")
        S.set(cx + lx, yv + 1, hz1 + 4, ding)
        S.set(cx + lx, yv, hz1 + 3, kl)
    Z.guhbeeld(S, cx, yv, hz1 + 1, 5)
    # a normal side door in the east wall (the big Knuffeldeur is for ceremonies)
    for y in (yv, yv + 1):
        S.set(hx2, y, cz - 2, "minecraft:air")
    S.set(hx2, yv, cz - 2, "roze_lakhout_deur[facing=east,half=lower,hinge=left,open=false]")
    S.set(hx2, yv + 1, cz - 2, "roze_lakhout_deur[facing=east,half=upper,hinge=left,open=false]")
    S.fill(hx2 + 1, yv - 1, cz - 4, hx2 + 4, yv - 1, cz, "tegel_hartjes")
    POI["ordetempel_binnen"] = w(cx, yv, cz - 2)
    POI["ordetempel_zijdeur"] = w(hx2 + 1, yv, cz - 2)
    POI["ordetempel_altaar"] = w(cx, yv, hz1 + 6)
    dx1, y0, fz1, dw, dh = info["deur"]
    DEUREN["ordetempel"] = dict(min=w(dx1, y0, fz1 + 1), max=w(dx1 + dw - 1, y0 + dh - 1, fz1 + 2), as_="x",
                                voor=w(cx, y0, fz1 + 5))


def main(uit):
    Z.terrein, Z.haven, Z.lift, Z.grondhuisjes = terrein, haven, lift, grondhuisjes
    Z.KLEIN[0] = (70, 128, 30, 8, "rood")
    S = SK.Scene(*Z.MAAT)
    info = Z.bouw(S)
    ordetempel_binnen(S, info)
    px, pz, pr = Z.PLEIN
    questbord(S, px - 9, pz - pr + 2)
    POI["questbord"] = w(px - 7, G + 1, pz - pr + 3)
    S.set(px - 14, G + 1, pz + 8, "minecraft:campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]")
    POI["kampvuur_zwamzoet"] = w(px - 14, G + 1, pz + 8)
    POI["plein"] = w(px, G + 1, pz + 9)
    POI["guhpoort"] = w(px, G + 1, 210)
    # Mika clue 1: black fluff under the west ring stall
    S.set(100, G + 1, 178, "minecraft:black_carpet")
    POI["mika_aanwijzing_1"] = w(100, G + 1, 178)
    POI["mika_spion_kraam"] = w(100, G + 1, 180)
    # Mika clue 5: the spy's lookout on the east cap edge
    cx, cz, T, r, _, _, p, _ = Z.ZWAMMEN["oost"]
    ux = int(cx + p * r - 2)
    S.set(ux, T + 1, cz, "minecraft:black_carpet")
    S.set(ux - 1, T + 1, cz, "mika_steen")
    S.set(ux - 1, T + 1, cz + 1, "minecraft:chest[facing=west,type=single,waterlogged=false]")
    POI["mika_aanwijzing_5"] = w(ux, T + 1, cz)
    for naam, z_ in Z.ZWAMMEN.items():
        POI[f"hoed_{naam}"] = w(z_[0], z_[2] + 1, z_[1])
    tuintjes(S, __import__("random").Random(5))
    S.fix_connections()
    terrein_weg(S)
    mis = Z.check(S)
    np.savez_compressed(uit, v=S.v, pal=np.array(S.pal), off=np.array([OX, OY, OZ]))
    with open(uit.replace(".npz", ".json"), "w", encoding="utf-8") as f:
        json.dump(dict(poi=POI, deuren=DEUREN, gebouwen=GEBOUWEN, ontbreekt=mis), f, indent=1)
    print("stad:", int((S.v > 0).sum()), "blokken")


if __name__ == "__main__":
    main(sys.argv[1])
