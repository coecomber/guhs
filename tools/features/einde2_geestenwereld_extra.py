"""
einde2, slice geest_bouw (CONTRACT_E2_B 4.5 + 7.8): everything of the Geestenwereld revamp outside the White Lotus:

  - Koh's eerie corridor (koh_gang): arches of black roots with faces on them; the watching masks in his cave
    (kijkmasker_1..12, entities that turn their face to you);
  - the islet by Tui and La (tui_la_eilandje), reached over stepping stones through the Hemelvijver;
  - the tea garden of the Theepaviljoen (theetuin) with the gossip club of the Gefluisterwoud (roddel_1..4);
  - the misty swamp corner of the Gefluisterwoud (moeras, moeras_visioen), and from its edge, far off in the mist, the 8th
    island (eiland_8: seen, never reached; its path is the overlay laag_pad_8, its secret is geest_spel's);
  - the Geestenklok: a giant old bell hanging from a beam between two islets, right over the road to the Hemelvijver;
  - the sights on the long paths (bezienswaardigheid_1..8), the path lanterns that geest_spel switches off per player
    (padlantaarn_<zone>_<n>), the footprints to three secrets (voetstappen_1..3, geheim_1..3), the chakra path
    (chakra_1..5 here, 6 and 7 at the Lotus and the Top; Guru Pathik-guh at the first), the mirror ponds, the spots where
    the hooded Wan-guh stands far off (wan_1..5), the giants' far spots (geestreus_1..4), a few light hills (heuvel_*);
  - the overlays laag_woud_bloei (the forest in bloom) and laag_lantaarnboom_licht (the lantern tree all lit), shown per
    player by geest_spel.
"""
import math

from features import einde2_geestenwereld_bouw as g
from features import einde2_geestenwereld_tekst as tekst

M, WOUD, KOH, VIJVER, THEE = g.M, g.WOUD, g.KOH, g.VIJVER, g.THEE
GRAS, AARDE, STEEN, TEGEL, PLAAT = g.GRAS, g.AARDE, g.STEEN, g.TEGEL, g.PLAAT
HOUT, SHOUT, PLANK, SPLANK = g.HOUT, g.SHOUT, g.PLANK, g.SPLANK
CYAAN, ROZE, SCHADUW, NEVEL, LOTUSBLAD, LANTAARN = g.CYAAN, g.ROZE, g.SCHADUW, g.NEVEL, g.LOTUSBLAD, g.LANTAARN
HOED, GEDENK, MASKER, WATER = g.HOED, g.GEDENK, g.MASKER, g.WATER
_hash = g._hash
LAMP_HANG = {"hanging": "true", "waterlogged": "false"}
LAMP_AAN = {"hanging": "false", "waterlogged": "false"}

EILAND_8 = dict(x=54, z=480, f=62, r=24)
MOERAS = dict(x=104, z=388, f=48, r=18)
VOETSTAPPEN_1 = [(M["x"] + 6, M["z"] + 19), (M["x"] + 16, M["z"] + 14), (M["x"] + 21, M["z"] + 4), (M["x"] + 22, M["z"] - 7)]
VOETSTAPPEN_2 = [(WOUD["x"] + 46, WOUD["z"] - 2), (WOUD["x"] + 36, WOUD["z"] + 10), (WOUD["x"] + 30, WOUD["z"] + 22), (WOUD["x"] + 22, WOUD["z"] + 28)]
VOETSTAPPEN_3 = [(THEE["x"] + 2, THEE["z"] + 12), (THEE["x"] + 4, THEE["z"] + 20), (THEE["x"] + 14, THEE["z"] + 30), (THEE["x"] + 11, THEE["z"] + 42)]
HOLLE_BOOM = (WOUD["x"] + 20, WOUD["z"] + 30)
SPIEGEL_1 = (M["x"] - 17, M["z"] + 15)
SPIEGEL_2 = (WOUD["x"] + 34, WOUD["z"] + 18)
CHAKRA_2 = (WOUD["x"] + 6, WOUD["z"] - 18)


def vooraf(b):
    """Before the zones are built: keep the trees of the Meditatiecirkel and the woud out of the spots of the revamp."""
    for route in (VOETSTAPPEN_1, VOETSTAPPEN_2):
        for (ax, az), (bx, bz) in zip(route, route[1:]):
            n = max(1, round(math.hypot(bx - ax, bz - az) / 2))
            for k in range(n + 1):
                b.vrij.append((ax + (bx - ax) * k / n, az + (bz - az) * k / n, 2))
    b.vrij += [(HOLLE_BOOM[0], HOLLE_BOOM[1], 6), (SPIEGEL_1[0], SPIEGEL_1[1], 5), (SPIEGEL_2[0], SPIEGEL_2[1], 5), (CHAKRA_2[0], CHAKRA_2[1], 4),
               (M["x"] + 13, M["z"] + 25, 3), (M["x"] + 22, M["z"] - 8, 3), (MOERAS["x"], MOERAS["z"], MOERAS["r"] + 4)]


def steen(b, id, x, f, z, facing, regels=None):
    """A memorial stone with its lore (and a standing sign with its name on it, when given)."""
    if regels:
        g.gedenksteen(b, id, x, f, z, facing, regels)
    else:
        b.set(x, f, z, GEDENK, {"facing": facing})
        b.prop(f"lore_{id}", x, f, z, wie=f"gedenksteen: {tekst.LORE[id][0]}")


def grond(b, x, y, z):
    """Tiles under a spot (where nothing is yet)."""
    b.zet(x, y - 1, z, TEGEL)
    b.zet(x, y - 2, z, STEEN)


# =====================================================================================================================
# Koh: the corridor and the watching masks
# =====================================================================================================================
def koh_gang(b):
    cx, cz, f = KOH["x"], KOH["z"], KOH["f"]
    z0, z1 = cz + 25, cz + 40
    for z in range(z0, z1 + 1):
        for x in range(cx - 1, cx + 2):
            b.set(x, f - 1, z, TEGEL if (z + x) % 3 else STEEN)
            b.zet(x, f - 2, z, STEEN)
    for k, z in enumerate(range(z0, z1 + 1, 3)):
        for x in (cx - 3, cx + 3):
            grond(b, x, f, z)
            for y in range(f, f + 5):
                b.set(x, y, z, SHOUT, {"axis": "y"})
        for x in range(cx - 3, cx + 4):
            b.set(x, f + 5 + (1 if abs(x - cx) <= 1 else 0), z, SHOUT, {"axis": "x"})
            if abs(x - cx) > 1:
                b.set(x, f + 6, z, SCHADUW) if _hash(x, z, 77) < 0.5 else None
        for x in range(cx - 1, cx + 2):
            b.set(x, f + 5, z, SHOUT, {"axis": "x"})
        # a face on each pillar, looking into the corridor, a different one each arch
        b.set(cx - 2, f + 2, z, MASKER, {"facing": "east", "gezicht": str((k * 5) % 12)})
        b.set(cx + 2, f + 2, z, MASKER, {"facing": "west", "gezicht": str((k * 7 + 3) % 12)})
        for x in (cx - 1, cx + 1):                                      # roots hanging down
            if _hash(x, z, 78) < 0.7:
                for y in range(f + 4, f + 2 + int(_hash(x, z, 79) * 2), -1):
                    b.zet(x, y, z, SCHADUW)
        if k % 2 == 0:
            b.zet(cx - 3, f, z + 1, HOED)
    b.zone("koh_gang", cx - 3, f, z0, cx + 3, f + 6, z1, wie="de Gang der Gezichten: een eng-mooie gang naar Koh's Grot")
    grond(b, cx + 5, f, z1 + 2)
    steen(b, "koh_gang", cx + 5, f, z1 + 2, "west", ["Gang der", "Gezichten"])
    b.prop("chakra_3", cx + 10, f, cz + 30, wie="chakra 3: mediteren bij Koh (geen emotie, ook niet van binnen)")
    g.chakra_mat(b, cx + 10, f, cz + 30)


def kijkmaskers(b):
    """Twelve masks in Koh's room that turn their face towards you (entities guhs:kijkmasker, Soort = the face 0-11)."""
    cx, cz, f = KOH["x"], KOH["z"], KOH["f"]
    for k in range(12):
        th = -math.pi / 2 + (k - 5.5) * 0.42
        for rr in (13, 12, 11, 10):
            x, z = cx + round(math.cos(th) * rr), cz + round(math.sin(th) * rr)
            y = f + 2 + k % 3
            if b.leeg(x, y, z) and b.leeg(x, y + 1, z):
                break
        dx, dz = cx - x, cz - z
        yaw = math.degrees(math.atan2(-dx, dz))
        b.plek(f"kijkmasker_{k + 1}", x, y, z, yaw=round(yaw, 1), wie=f"een masker dat met je meedraait (gezicht {k})",
               wezen="guhs:kijkmasker", los=True)


# =====================================================================================================================
# the Hemelvijver: the islet by Tui and La
# =====================================================================================================================
def tui_la(b):
    cx, cz, f = VIJVER["x"], VIJVER["z"], VIJVER["f"]
    for x in range(cx - 5, cx + 6):
        for z in range(cz - 5, cz + 6):
            d = math.hypot(x - cx, z - cz)
            if d <= 4.3:
                for y in range(f - 10, f - 1):
                    b.set(x, y, z, STEEN)
                b.set(x, f - 1, z, GRAS if d < 3.3 else TEGEL)
                b.set(x, f - 2, z, AARDE)
                for y in range(f, f + 4):
                    b.s.blocks.pop((x, y, z), None)
    for sx in range(cx - 27, cx - 5, 4):                                  # stepping stones from the west rim (at the water)
        for x in range(sx, sx + 3):
            for z in range(cz - 1, cz + 2):
                b.s.blocks.pop((x, f - 1, z), None)
                b.set(x, f - 2, z, TEGEL)
                b.set(x, f - 3, z, STEEN)
    b.set(cx - 1, f, cz + 3, "minecraft:candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
    b.prop("tui_la_speler", cx, f, cz, wie="het eilandje midden in de Hemelvijver: Tui en La zwemmen vlak om je heen")
    b.zone("tui_la_eilandje", cx - 4, f, cz - 4, cx + 4, f + 6, cz + 4, wie="het eilandje dicht bij Tui en La")
    steen(b, "tui_la", cx + 3, f, cz, "west")
    b.prop("chakra_4", cx, f, cz - 31, wie="chakra 4: mediteren op de rand van de Hemelvijver")
    g.chakra_mat(b, cx, f, cz - 31)


# =====================================================================================================================
# the Theepaviljoen: the tea garden and the gossip club
# =====================================================================================================================
def theetuin(b):
    cx, cz, f = THEE["x"], THEE["z"], THEE["f"]
    tx, tz = cx, cz + 30
    g.eiland(b, tx, tz, f, 20, 14, 708)
    for z in range(cz + 10, cz + 44):                                    # a path of tiles south from the pavilion
        for x in range(cx - 1, cx + 2):
            if not b.leeg(x, f - 1, z) and b.leeg(x, f, z):
                b.set(x, f - 1, z, TEGEL if (z % 2) else STEEN)
    tafels = [((cx - 8, cz + 28), ("roddel_1", "guhs:lantaarngeest", -2, "east"), ("roddel_2", "guhs:libelkonijnguh", 2, "west")),
              ((cx + 8, cz + 28), ("roddel_3", "guhs:libelkonijnguh", -2, "east"), ("roddel_4", None, 2, "west"))]
    for (x, z), *gasten in tafels:
        b.set(x, f, z, PLANK)
        b.set(x, f + 1, z, "minecraft:decorated_pot", {"facing": "north", "waterlogged": "false", "cracked": "false"})
        b.set(x, f, z + 1, PLAAT, {"type": "bottom", "waterlogged": "false"})
        b.set(x, f, z - 1, PLAAT, {"type": "bottom", "waterlogged": "false"})
        for id, wezen, dx, kijk in gasten:
            if wezen:
                b.plek(id, x + dx, f, z, yaw=g.YAW[kijk], wie="de roddelclub van het Gefluisterwoud, aan de thee", wezen=wezen)
            else:
                b.plek(id, x + dx, f, z, yaw=g.YAW[kijk], wie="de roddelclub van het Gefluisterwoud: de Theegeest zelf schuift aan",
                       kind="THEEGEEST_GUH")
            b.vrij.append((x + dx, z, 2))
    for x, z in ((cx - 12, cz + 22), (cx + 12, cz + 22), (cx - 12, cz + 36), (cx + 12, cz + 36)):
        g.lantaarnpaal(b, x, f, z)
    for k, (x, z) in enumerate(((cx - 16, cz + 34), (cx + 16, cz + 32))):
        g.boom(b, x, f, z, 5, 3, ROZE, 720 + k, hangers=False)
    g.spiegelvijver(b, "spiegelvijver_3", cx, f, cz + 38)
    b.prop("chakra_5", cx - 10, f, cz + 40, wie="chakra 5: mediteren in de theetuin")
    g.chakra_mat(b, cx - 10, f, cz + 40)
    steen(b, "theetuin", cx - 3, f, cz + 14, "south", ["De theetuin"])
    b.zone("theetuin", cx - 18, f, cz + 12, cx + 18, f + 6, cz + 46, wie="de theetuin van het Theepaviljoen (de roddelclub)")


# =====================================================================================================================
# the swamp corner of the woud and the 8th island
# =====================================================================================================================
def moeras(b):
    cx, cz, f, r = MOERAS["x"], MOERAS["z"], MOERAS["f"], MOERAS["r"]
    g.eiland(b, cx, cz, f, r, 14, 211, top=AARDE)
    b.vrij.append((cx, cz, r + 4))
    for x in range(cx - r - 3, cx + r + 4):
        for z in range(cz - r - 3, cz + r + 4):
            if b.s.blocks.get((x, f - 1, z), ("",))[0] not in (AARDE, GRAS) or math.hypot(x - cx, z - cz) > r:
                continue
            n = g.ruis(x, z, 4, 212)
            if n > 0.6:
                b.set(x, f - 1, z, WATER, {"level": "0"})
                b.set(x, f - 2, z, "minecraft:mud")
                if _hash(x, z, 213) < 0.12:
                    b.set(x, f, z, "minecraft:lily_pad")
            elif n < 0.3:
                b.set(x, f - 1, z, "minecraft:mud")
            elif _hash(x, z, 214) < 0.05:
                b.zet(x, f, z, HOED)
    for k in range(7):                                                   # crooked roots like a mangrove, leaves hanging
        th = k * math.tau / 7 + 0.4
        a = (cx + round(math.cos(th) * (r - 5)), f, cz + round(math.sin(th) * (r - 5)))
        c = (cx + round(math.cos(th + 0.5) * (r - 9)), f, cz + round(math.sin(th + 0.5) * (r - 9)))
        top = ((a[0] + c[0]) // 2, f + 5 + k % 3, (a[2] + c[2]) // 2)
        g.lijn6(b, a, top, SHOUT, {"axis": "y"})
        g.lijn6(b, top, c, SHOUT, {"axis": "y"})
        for y in range(top[1] - 1, top[1] - 4, -1):
            b.zet(top[0], y, top[2], SCHADUW)
    for k in range(5):                                                    # wisps of mist rising from the water
        x, z = cx + round(math.cos(k * 1.3) * 8), cz + round(math.sin(k * 1.3) * 8)
        for y in range(f - 1, f + 3 + k % 3):
            b.set(x, y, z, NEVEL)
        b.set(x, f - 2, z, "minecraft:mud")
    # the vision pool: a still round pool in a ring of stones
    vx, vz = cx - 2, cz + 3
    for x in range(vx - 3, vx + 4):
        for z in range(vz - 3, vz + 4):
            d = math.hypot(x - vx, z - vz)
            if d <= 1.6:
                b.set(x, f - 1, z, WATER, {"level": "0"})
                b.set(x, f - 2, z, "minecraft:polished_blackstone")
            elif d <= 2.6:
                b.set(x, f - 1, z, "minecraft:mossy_cobblestone")
                for y in range(f, f + 3):
                    b.s.blocks.pop((x, y, z), None)
    b.prop("moeras_visioen", vx, f - 1, vz, wie="het stille poeltje in het moeras: heel even een visioen van een guh die je kent")
    b.zone("moeras", cx - r, f - 1, cz - r, cx + r, f + 6, cz + r, wie="het mistige moerashoekje van het Gefluisterwoud")
    steen(b, "moeras", vx + 4, f, vz - 2, "south", ["Het moeras"])
    grond(b, vx + 4, f, vz - 2)
    # the anchor of the path to the 8th island: the swamp's south-west edge
    e8 = EILAND_8
    th = math.atan2(e8["z"] - cz, e8["x"] - cx)
    ax, az = cx + round(math.cos(th) * (r - 3)), cz + round(math.sin(th) * (r - 3))
    for x in range(ax - 1, ax + 2):
        for z in range(az - 1, az + 2):
            b.set(x, f - 1, z, TEGEL)
            b.zet(x, f - 2, z, STEEN)
            for y in range(f, f + 3):
                b.s.blocks.pop((x, y, z), None)
    b.prop("pad_8_anker", ax, f, az, wie="hier begint het pad naar het 8e eiland (als het verschijnt)")
    b.pad8_anker = (ax, az)


def eiland_8(b):
    """The 8th island: far off in the mist, never reached by a path of the build (geest_spel shows laag_pad_8). The origin,
    where it all began: a ring of eight old pillars round a giant, ancient tree whose light and dark wood grew into each
    other, at its foot a cold camp (a fire, a mat, a little hut) and a pool of two colours."""
    cx, cz, f, r = EILAND_8["x"], EILAND_8["z"], EILAND_8["f"], EILAND_8["r"]
    voor = set(b.s.blocks)
    g.eiland(b, cx, cz, f, r, 20, 808, ruw=0.1)
    for k in range(8):                                                    # the eight pillars
        th = k * math.tau / 8 + 0.2
        x, z = cx + round(math.cos(th) * 17), cz + round(math.sin(th) * 17)
        hoog = 6 + int(_hash(k, 81) * 6)
        for y in range(f, f + hoog):
            b.set(x, y, z, "minecraft:chiseled_stone_bricks" if y == f + hoog - 1 else ("minecraft:cracked_stone_bricks" if _hash(x, y, 82) < 0.4 else "minecraft:stone_bricks"))
        if k % 2 == 0:
            b.set(x, f + hoog, z, LANTAARN, LAMP_AAN)
    # the old tree: two trunks, light and dark, turning round each other
    for y in range(f, f + 30):
        for kant, n in ((0, HOUT), (math.pi, SHOUT)):
            th = y * 0.32 + kant
            for dx in (0, 1):
                for dz in (0, 1):
                    b.set(cx + round(math.cos(th) * 1.5) + dx, y, cz + round(math.sin(th) * 1.5) + dz, n, {"axis": "y"})
    for y in range(f, f + 31):
        b.set(cx, y, cz, SHOUT if y % 2 else HOUT, {"axis": "y"})
    ky, kr = f + 33, 12
    for x in range(cx - kr - 1, cx + kr + 2):
        for z in range(cz - kr - 1, cz + kr + 2):
            for y in range(ky - 6, ky + 7):
                dd = math.sqrt((x - cx) ** 2 + ((y - ky) * 1.9) ** 2 + (z - cz) ** 2)
                if dd <= kr and _hash(x, y, z, 83) < 0.85:
                    b.zet(x, y, z, CYAAN if (x - cx) * (z - cz) >= 0 else SCHADUW)
    for y in range(f + 30, ky + 4):
        b.set(cx, y, cz, HOUT, {"axis": "y"})
    for k in range(8):
        th = k * math.tau / 8
        g.lijn6(b, (cx, ky - 2, cz), (cx + round(math.cos(th) * (kr - 2)), ky, cz + round(math.sin(th) * (kr - 2))), SHOUT, {"axis": "y"})
    # the camp at its foot (south): a cold fire, a worn mat, a little hut
    kx, kz = cx, cz + 8
    b.set(kx, f, kz, "minecraft:campfire", {"lit": "false", "facing": "south", "signal_fire": "false", "waterlogged": "false"})
    b.set(kx + 2, f, kz, "minecraft:white_carpet")
    b.set(kx + 2, f, kz + 1, "minecraft:white_carpet")
    for x in range(kx - 6, kx - 2):
        for z in range(kz - 1, kz + 3):
            b.set(x, f - 1, z, PLANK)
            if x in (kx - 6, kx - 3) or z in (kz - 1, kz + 2):
                if not (x == kx - 3 and z == kz + 1):
                    for y in range(f, f + 3):
                        b.set(x, y, z, SPLANK)
            b.set(x, f + 3, z, PLAAT, {"type": "bottom", "waterlogged": "false"})
    # the pool of two colours (west)
    px, pz = cx - 9, cz - 4
    for x in range(px - 3, px + 4):
        for z in range(pz - 3, pz + 4):
            d = math.hypot(x - px, z - pz)
            if d <= 2.6:
                licht = math.sin(math.atan2(z - pz, x - px) + d * 1.4) > 0
                b.set(x, f - 1, z, WATER, {"level": "0"})
                b.set(x, f - 2, z, "minecraft:white_concrete" if licht else "minecraft:black_concrete")
            elif d <= 3.6:
                b.set(x, f - 1, z, TEGEL)
    b.prop("wan_onthulling", kx, f, kz + 3, wie="hier, aan de voet van de oude boom, onthult Wan-guh de oorsprong", los=True)
    b.camera("wan", [(cx + 4, f + 9, cz + 24), (cx + 1, f + 5, cz + 18)], (kx, f + 1, kz + 3), zicht=["wan_onthulling"],
             wie="de camera van de onthulling op het 8e eiland: van het zuiden naar de voet van de oude boom")
    b.zone("eiland_8", cx - r, f - 20, cz - r, cx + r, f + 40, cz + r, wie="het 8e eiland: je ziet het vaag in de mist, je komt er niet")
    b.los_cellen |= set(b.s.blocks) - voor


def laag_pad_8(b):
    """The overlay: the path that appears from the swamp to the 8th island (blocks only, only where the build is air)."""
    ax, az = b.pad8_anker
    e8 = EILAND_8
    th = math.atan2(az - e8["z"], ax - e8["x"])
    ex, ez = e8["x"] + round(math.cos(th) * (e8["r"] - 4)), e8["z"] + round(math.sin(th) * (e8["r"] - 4))
    voor = dict(b.s.blocks)
    n_paden = len(b.paden)
    g.pad(b, [(ax, az), ((ax + ex) / 2 + 6, (az + ez) / 2), (ex, ez)], MOERAS["f"], e8["f"], half=1, lantaarn_elke=14, lotus_elke=20, seed=880)
    del b.paden[n_paden:]
    nieuw = {c: v for c, v in b.s.blocks.items() if c not in voor}
    for c in nieuw:
        del b.s.blocks[c]
    return laag(b, "pad_8", nieuw)


def laag(b, naam, cellen):
    """An overlay template of these cells (only cells where the build is air)."""
    cellen = {c: v for c, v in cellen.items() if c not in b.s.blocks}
    xs, ys, zs = zip(*cellen)
    x0, y0, z0 = min(xs), min(ys), min(zs)
    s = b.h.Structure((max(xs) - x0 + 1, max(ys) - y0 + 1, max(zs) - z0 + 1))
    for (x, y, z), (n, props, nbt) in cellen.items():
        s.set(x - x0, y - y0, z - z0, n, props, nbt)
    b.laag(naam, s, x0, y0, z0)
    return len(cellen)


# =====================================================================================================================
# the Geestenklok
# =====================================================================================================================
def geestenklok(b, info):
    """A giant old bell of green copper, hanging from a beam between two islets, high over the road; its rope comes down to
    a little landing beside the road (the prop geestenklok: ring it there)."""
    midden, totaal = info["midden"], info["totaal"]
    px, pz, h2, nx, nz, dd = min(midden, key=lambda m: abs(m[5] - totaal * 0.6))
    x, z = round(px), round(pz)
    y = h2 // 2 + (1 if h2 % 2 else 0)
    # the road runs about east-west here: the islets north and south of it
    voor = set(b.s.blocks)
    for kant in (-1, 1):
        iz = z + kant * 20
        g.eiland(b, x, iz, y, 7, 12, 640 + kant)
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                for yy in range(y, y + 26):
                    hoek = abs(dx) == 1 and abs(dz) == 1
                    b.set(x + dx, yy, iz + dz, HOUT if hoek else TEGEL, {"axis": "y"} if hoek else None)
        b.set(x, y + 26, iz, LANTAARN, LAMP_AAN)
    for zz in range(z - 21, z + 22):                                   # the beam
        for dx in (-1, 0, 1):
            b.set(x + dx, y + 24, zz, SPLANK)
            b.set(x + dx, y + 25, zz, SPLANK if dx == 0 else PLAAT, None if dx == 0 else {"type": "bottom", "waterlogged": "false"})
    # the bell: open at the bottom, a band of gold near its lip, a clapper inside
    for yy in range(y + 9, y + 24):
        t = (y + 23 - yy)                                               # 0 at the top
        straal = 2.0 if t == 0 else (3.0 if t < 3 else (4.0 if t < 8 else (4.6 if t < 12 else 5.4)))
        for dx in range(-6, 7):
            for dz in range(-6, 7):
                d = math.hypot(dx, dz)
                if d <= straal + 0.2 and (d > straal - 1.1 or t == 0):
                    n = "minecraft:waxed_oxidized_copper" if t < 11 else ("minecraft:gold_block" if t == 11 else "minecraft:waxed_weathered_copper")
                    b.set(x + dx, yy, z + dz, n)
    for yy in range(y + 13, y + 23):
        b.set(x, yy, z, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    b.set(x, y + 12, z, "minecraft:gold_block")
    b.set(x, y + 11, z, "minecraft:gold_block")
    # the landing beside the road and the rope from the beam down to it
    rz = z + 8
    for dx in (-1, 0, 1):
        for zz in range(z + 2, z + 10):
            b.zet(x + dx, y - 1, zz, TEGEL)
            b.zet(x + dx, y - 2, zz, STEEN)
    for yy in range(y, y + 24):
        b.set(x, yy, rz, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    b.prop("geestenklok", x, y + 1, rz, wie="de Geestenklok: het touw (luiden); de reusachtige klok hangt erboven, over de weg")
    b.zone("klok", x - 6, y, z - 6, x + 6, y + 25, z + 10, wie="de Geestenklok tussen twee eilandjes")
    steen(b, "geestenklok", x - 2, y, rz + 1, "north")
    grond(b, x - 2, y, rz + 1)
    b.los_cellen |= {c for c in set(b.s.blocks) - voor if b.s.blocks[c][0] != "minecraft:chain"}


# =====================================================================================================================
# sights along the long paths
# =====================================================================================================================
def langs(info, frac):
    m = min(info["midden"], key=lambda m: abs(m[5] - info["totaal"] * frac))
    px, pz, h2, nx, nz, dd = m
    return px, pz, h2 // 2 + (1 if h2 % 2 else 0), nx, nz


def zijplatform(b, info, frac, kant, afstand, r, seed):
    """A little platform beside the path (an islet with a top of tiles) and a bridge of tiles to it. Returns its middle,
    the y you stand at and the way that looks back to the path."""
    px, pz, y, nx, nz = langs(info, frac)
    sx, sz = round(px + nx * kant * afstand), round(pz + nz * kant * afstand)
    g.eiland(b, sx, sz, y, r, 6 + r, seed, top=TEGEL, ruw=0.05)
    for k in range(2, int(afstand) + 1):
        x, z = round(px + nx * kant * k), round(pz + nz * kant * k)
        for dd in (0, 1):
            xx, zz = (x, z) if dd == 0 else ((x + 1, z) if abs(nz) > abs(nx) else (x, z + 1))
            b.zet(xx, y - 1, zz, TEGEL)
            b.zet(xx, y - 2, zz, STEEN)
    kijk = (-nx * kant, -nz * kant)
    facing = ("east" if kijk[0] > 0 else "west") if abs(kijk[0]) > abs(kijk[1]) else ("south" if kijk[1] > 0 else "north")
    return sx, y, sz, facing


def visser(b, n, info, frac, kant, wezen, soort_id):
    """A fishing spirit: a little jetty, a seat, a rod over the edge with a glowing float hanging down into the mist."""
    sx, y, sz, facing = zijplatform(b, info, frac, kant, 7, 3, 850 + n)
    fx, fz = g.DIRS[facing]
    ux, uz = -fx, -fz                                        # away from the path, over the edge
    ex, ez = sx + ux * 3, sz + uz * 3
    for k in range(1, 7):
        b.zet(sx + ux * k, y - 1, sz + uz * k, PLANK)
    px, pz = sx + ux * 6, sz + uz * 6
    for yy in range(y, y + 3):
        b.set(px, yy, pz, "minecraft:bamboo_block", {"axis": "y"})
    b.set(px + ux, y + 2, pz + uz, PLAAT, {"type": "top", "waterlogged": "false"})
    b.set(px + ux * 2, y + 2, pz + uz * 2, PLAAT, {"type": "top", "waterlogged": "false"})
    for yy in range(y - 4, y + 2):
        b.set(px + ux * 2, yy, pz + uz * 2, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    b.set(px + ux * 2, y - 5, pz + uz * 2, LANTAARN, LAMP_HANG)
    b.los_cellen |= {(px + ux * 2, yy, pz + uz * 2) for yy in range(y - 5, y + 2)}
    b.set(sx + ux * 5, y, sz + uz * 5, PLAAT, {"type": "bottom", "waterlogged": "false"})
    b.plek(soort_id, sx + ux * 4, y, sz + uz * 4, yaw=g.YAW[{"north": "south", "south": "north", "east": "west", "west": "east"}[facing]],
           wie="een vissende geest (vist in de nevel, vangt nooit iets, is heel tevreden)", wezen=wezen)
    b.prop(f"bezienswaardigheid_{n}", sx, y, sz, wie="een vissende geest aan het eind van een steigertje")


def schrijn(b, n, info, frac, kant):
    """A little shrine: two posts, a roof, a small guh with ears of stone, a knabbel (a pot) and candles as an offering."""
    sx, y, sz, facing = zijplatform(b, info, frac, kant, 7, 3, 850 + n)
    fx, fz = g.DIRS[facing]
    ux, uz = -fx, -fz
    vx, vz = -uz, ux
    bx, bz = sx + ux, sz + uz
    for s in (-1, 1):
        for yy in range(y, y + 4):
            b.set(bx + vx * s * 2, yy, bz + vz * s * 2, HOUT, {"axis": "y"})
    for s in range(-3, 4):
        for d in (-1, 0, 1):
            b.set(bx + vx * s + ux * d, y + 4, bz + vz * s + uz * d, SPLANK if d == 0 or abs(s) < 3 else PLAAT,
                  None if d == 0 or abs(s) < 3 else {"type": "bottom", "waterlogged": "false"})
    b.set(bx, y, bz, TEGEL)
    b.set(bx, y + 1, bz, "minecraft:pink_concrete")                     # a little guh: a head with two ears
    b.set(bx + vx, y + 2, bz + vz, "minecraft:pink_concrete")
    b.set(bx - vx, y + 2, bz - vz, "minecraft:pink_concrete")
    b.set(bx + vx, y + 1, bz + vz, "minecraft:pink_concrete")
    b.set(bx - vx, y + 1, bz - vz, "minecraft:pink_concrete")
    b.set(bx - ux, y, bz - uz, "minecraft:decorated_pot", {"facing": facing, "waterlogged": "false", "cracked": "false"})
    b.set(bx - ux + vx * 2, y, bz - uz + vz * 2, "minecraft:candle", {"candles": "3", "lit": "true", "waterlogged": "false"})
    b.set(bx - ux - vx * 2, y, bz - uz - vz * 2, "minecraft:candle", {"candles": "2", "lit": "true", "waterlogged": "false"})
    steen(b, "schrijn", sx + fx * 2 + vx * 2, y, sz + fz * 2 + vz * 2, facing)
    b.prop(f"bezienswaardigheid_{n}", sx + fx, y, sz + fz, wie="een schrijntje met een knabbel als offer")


def raadselstenen(b, n, info, frac, kant, lore_id):
    """Floating stones with a riddle: three stones that hang in the air beside a platform, and a memorial stone with the
    riddle (and, very small at the bottom, its answer)."""
    sx, y, sz, facing = zijplatform(b, info, frac, kant, 7, 3, 850 + n)
    fx, fz = g.DIRS[facing]
    ux, uz = -fx, -fz
    vx, vz = -uz, ux
    for k, (d, s, h) in enumerate(((6, -2, 3), (7, 1, 6), (5, 3, 9))):
        cxx, czz = sx + ux * d + vx * s, sz + uz * d + vz * s
        for dx in (0, 1):
            for dz in (0, 1):
                for dy in (0, 1):
                    if not (dx and dz and dy):
                        b.set(cxx + dx, y + h + dy, czz + dz, "minecraft:chiseled_stone_bricks" if (dx + dy + dz) == 0 else STEEN)
                        b.los_cellen.add((cxx + dx, y + h + dy, czz + dz))
    steen(b, lore_id, sx + ux, y, sz + uz, facing)
    b.prop(f"bezienswaardigheid_{n}", sx, y, sz, wie="zwevende stenen met een raadsel")


def waterval_omhoog(b, n, info, frac, kant, hoog):
    """A waterfall that falls up out of a little pool beside the path."""
    sx, y, sz, facing = zijplatform(b, info, frac, kant, 7, 4, 850 + n)
    fx, fz = g.DIRS[facing]
    ux, uz = -fx, -fz
    wx, wz = sx + ux * 2, sz + uz * 2
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            b.set(wx + dx, y - 1, wz + dz, WATER, {"level": "0"})
            b.set(wx + dx, y - 2, wz + dz, STEEN)
    for yy in range(y - 1, y + hoog):
        b.set(wx, yy, wz, NEVEL)
        if yy < y + 6:
            b.set(wx + ux, yy, wz + uz, NEVEL)
    b.set(sx + fx, y, sz + fz, PLAAT, {"type": "bottom", "waterlogged": "false"})
    b.prop(f"bezienswaardigheid_{n}", sx + fx, y, sz + fz, wie="een waterval die omhoog valt (en een plekje om te kijken)")


def maskerpoort(b, n, info, frac):
    """An old arch over the path with one big mask on it, looking down the path: Koh is not far."""
    px, pz, y, nx, nz = langs(info, frac)
    x, z = round(px), round(pz)
    dirx, dirz = nz, -nx                              # along the path
    for s in range(-3, 4):
        grond(b, round(px + nx * s), y, round(pz + nz * s))
    for s in (-3, 3):
        ax, az = round(px + nx * s), round(pz + nz * s)
        for yy in range(y, y + 6):
            b.set(ax, yy, az, SHOUT, {"axis": "y"})
    for s in range(-3, 4):
        b.set(round(px + nx * s), y + 6, round(pz + nz * s), SHOUT, {"axis": "x"})
        b.zet(round(px + nx * s), y + 7, round(pz + nz * s), SCHADUW)
    facing = ("east" if dirx > 0 else "west") if abs(dirx) > abs(dirz) else ("south" if dirz > 0 else "north")
    fx, fz = g.DIRS[facing]
    b.set(x + fx, y + 6, z + fz, MASKER, {"facing": facing, "gezicht": str(g.MASKERS.index("noh"))})
    terug = {"north": "south", "south": "north", "east": "west", "west": "east"}[facing]
    b.set(x - fx, y + 6, z - fz, MASKER, {"facing": terug, "gezicht": str(g.MASKERS.index("uil"))})
    b.prop(f"bezienswaardigheid_{n}", round(px + nx * 2), y, round(pz + nz * 2), wie="een oude poort over het pad met een masker erop: Koh is niet ver")


def bezienswaardigheden(b):
    P = b.pad_info
    visser(b, 1, P["m_woud"][0], 0.5, 1, "guhs:lantaarngeest", "visgeest_1")
    schrijn(b, 2, P["m_vijver"][0], 0.28, -1)
    raadselstenen(b, 3, P["m_thee"][0], 0.33, 1, "raadsel_1")
    waterval_omhoog(b, 4, P["m_thee"][0], 0.72, -1, 26)
    maskerpoort(b, 5, P["woud_koh"][0], 0.45)
    raadselstenen(b, 6, P["lotus_koh"][0], 0.5, -1, "raadsel_2")
    waterval_omhoog(b, 7, P["vijver_fang"][0], 0.45, 1, 34)
    visser(b, 8, P["thee_vijver"][0], 0.5, -1, "guhs:libelkonijnguh", "visgeest_2")


def padlantaarns(b):
    """The three lanterns nearest each zone on every path into it: padlantaarn_<zone>_<n> (the lantern block itself;
    geest_spel switches them off for a player for whom the zone is closed). Not for the Meditatiecirkel (always open)."""
    teller = {}
    for naam, (info, za, zb) in b.pad_info.items():
        lampen = sorted(info["lantaarns"], key=lambda l: l[1])
        for zone, rij in ((za, lampen[:3]), (zb, lampen[::-1][:3])):
            if zone == "meditatiecirkel":
                continue
            if len(rij) < 3 or (za != "meditatiecirkel" and zb != "meditatiecirkel" and len(lampen) < 6):
                b.fouten.append(f"pad {naam}: te weinig lantaarns voor {zone} ({len(lampen)})")
            for (x, y, z), dd in rij:
                teller[zone] = teller.get(zone, 0) + 1
                b.prop(f"padlantaarn_{zone}_{teller[zone]}", x, y, z, wie=f"padlantaarn naar {zone} (gaat uit als de zone dicht is)", los=True)


# =====================================================================================================================
# footprints, secrets, chakras, mirror ponds, Wan-guh, the giants, the masks, the herons
# =====================================================================================================================
def voetstappen(b, n, punten, f, wie):
    pad = []
    for (ax, az), (bx, bz) in zip(punten, punten[1:]):
        lengte = max(1, round(math.hypot(bx - ax, bz - az) / 2))
        for k in range(lengte):
            pad.append((round(ax + (bx - ax) * k / lengte), f, round(az + (bz - az) * k / lengte)))
    pad.append((punten[-1][0], f, punten[-1][1]))
    b.pad(f"voetstappen_{n}", pad, wie=wie)
    for x, _, z in pad:
        b.vrij.append((x, z, 2))


def geheim_offer(b, n, x, f, z, facing):
    """A secret: a tiny hidden shrine with a knabbel (a pot) as an offering, a lotus leaf mat, a candle, its own stone."""
    b.set(x, f - 1, z, LOTUSBLAD)
    fx, fz = g.DIRS[facing]
    vx, vz = -fz, fx
    b.set(x - fx, f, z - fz, "minecraft:decorated_pot", {"facing": facing, "waterlogged": "false", "cracked": "false"})
    b.set(x - fx + vx, f, z - fz + vz, "minecraft:candle", {"candles": "1", "lit": "true", "waterlogged": "false"})
    steen(b, f"geheim_{n}", x - fx - vx, f, z - fz - vz, facing)
    b.prop(f"geheim_{n}", x, f, z, wie="een geheimpje (waar de voetstappen heen gaan)")


def geheimen(b):
    mx, mz, mf = M["x"], M["z"], M["f"]
    voetstappen(b, 1, VOETSTAPPEN_1, mf,
                "gloeiende pootjes van iets onzichtbaars: van de aankomst om de vijver heen naar een geheimpje")
    geheim_offer(b, 1, mx + 22, mf, mz - 8, "south")
    wx, wz, wf = WOUD["x"], WOUD["z"], WOUD["f"]
    voetstappen(b, 2, VOETSTAPPEN_2, wf,
                "gloeiende pootjes door het woud naar een holle boom")
    holle_boom(b, HOLLE_BOOM[0], wf, HOLLE_BOOM[1])
    tx, tz, tf = THEE["x"], THEE["z"], THEE["f"]
    voetstappen(b, 3, VOETSTAPPEN_3, tf,
                "gloeiende pootjes door de theetuin naar een bankje aan de rand")
    geheim_offer(b, 3, tx + 11, tf, tz + 43, "north")
    b.set(tx + 12, tf, tz + 44, "minecraft:spruce_stairs", {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"})


def holle_boom(b, x, f, z):
    """Secret 2: an old hollow tree in the woud; inside a glowing mushroom, a lantern and a sleeping kennisvosje."""
    b.vrij.append((x, z, 6))
    for y in range(f, f + 9):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                d = math.hypot(dx, dz)
                if 1.5 < d <= 2.3 and not (dz == 2 and dx == 0 and y < f + 3) and not (dz == 2 and abs(dx) == 1 and y < f + 2):
                    b.set(x + dx, y, z + dz, SHOUT if (y + dx) % 3 else HOUT, {"axis": "y"})
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            for dy in range(0, 4):
                if math.sqrt(dx * dx + dz * dz + (dy * 1.5) ** 2) <= 3.4:
                    b.zet(x + dx, f + 9 + dy, z + dz, SCHADUW if (dx + dz) % 2 else CYAAN)
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            b.set(x + dx, f + 8, z + dz, SHOUT, {"axis": "y"})
    b.set(x + 1, f, z - 1, HOED)
    b.set(x, f + 7, z, LANTAARN, LAMP_HANG)
    b.prop("geheim_2", x, f, z, wie="een geheimpje: de holle boom")
    b.plek("kennisvos_4", x - 1, f, z, wie="een kennisvosje dat hier slaapt (en je de weg wijst)", wezen="guhs:kennisvosje")
    steen(b, "geheim_2", x, f, z - 1, "south")


def chakras(b):
    mx, mz, mf = M["x"], M["z"], M["f"]
    b.prop("chakra_1", mx + 12, mf, mz + 26, wie="chakra 1: de eerste meditatie, bij de aankomst (Guru Pathik-guh staat ernaast)")
    g.chakra_mat(b, mx + 12, mf, mz + 26)
    b.plek("guru_pathik", mx + 14, mf, mz + 24, yaw=g.YAW["west"], wie="Guru Pathik-guh als echo, bij het eerste chakra", kind="GURU_PATHIK_GUH")
    b.vrij.append((mx + 13, mz + 25, 3))
    wx, wz, wf = WOUD["x"], WOUD["z"], WOUD["f"]
    b.prop("chakra_2", CHAKRA_2[0], wf, CHAKRA_2[1], wie="chakra 2: mediteren in het Gefluisterwoud")
    g.chakra_mat(b, CHAKRA_2[0], wf, CHAKRA_2[1])
    g.spiegelvijver(b, "spiegelvijver_1", SPIEGEL_1[0], mf, SPIEGEL_1[1])
    g.spiegelvijver(b, "spiegelvijver_2", SPIEGEL_2[0], wf, SPIEGEL_2[1])


WAN_EILANDJES = [(250, 230), (40, 230), (410, 220), (580, 250), (560, 420)]


def wan_plekken(b):
    """Far spots in different zones where the hooded Wan-guh stands (he goes when you come close)."""
    for i, (ex, ez) in enumerate(WAN_EILANDJES, 1):
        x0, z0, f, r, wat = next(e for e in g.EILANDJES if (e[0], e[1]) == (ex, ez))
        for th in (math.pi, 0.0, math.pi / 2, -math.pi / 2):
            x, z = x0 + round(math.cos(th) * (r - 2)), z0 + round(math.sin(th) * (r - 2))
            if b.leeg(x, f, z) and b.leeg(x, f + 1, z) and not b.leeg(x, f - 1, z):
                break
        b.prop(f"wan_{i}", x, f, z, wie="een ver eilandje waar de gestalte met de kap (Wan-guh) staat (verdwijnt als je dichtbij komt)", los=True)


def geestreuzen(b):
    for i, (x, y, z) in enumerate(((14, 96, 300), (300, 112, 12), (594, 90, 300), (300, 100, 596)), 1):
        b.prop(f"geestreus_{i}", x, y, z, wie="heel ver weg: hier drijft een reusachtig geest-silhouet (geestentij)", los=True)


def mistreigers(b):
    kolommen = {}
    for (xx, yy, zz) in b.s.blocks:
        if (xx, zz) in b.mistreiger:
            kolommen[(xx, zz)] = max(kolommen.get((xx, zz), 0), yy)
    for i, (x, z) in enumerate(b.mistreiger, 1):
        y = kolommen[(x, z)] + 1
        b.plek(f"mistreiger_{i}", x, y, z, yaw=float((i * 77) % 360 - 180), wie="een mistreiger op het dak (vliegt op als je dichtbij komt)",
               wezen="guhs:mistreiger", los=True)


def heuvels(b):
    """A few light hills where the floor is flat and nothing stands (the flat lila floor stays elsewhere)."""
    lijst = [("heuvel_meditatiecirkel", M["x"] - 19, M["z"] - 12, 8, 3, M["f"]),
             ("heuvel_koh_grot", KOH["x"] + 26, KOH["z"] - 22, 9, 4, KOH["f"]),
             ("heuvel_theepaviljoen", THEE["x"] - 18, THEE["z"] - 8, 7, 3, THEE["f"])]
    plekken = [tuple(e["pos"]) for e in b.plekken.values() if "pos" in e and e["soort"] in ("npc", "prop")]
    for id, hx, hz, r, h, f in lijst:
        for x in range(hx - r, hx + r + 1):
            for z in range(hz - r, hz + r + 1):
                d = math.hypot(x - hx, z - hz)
                if d > r or b.s.blocks.get((x, f - 1, z), ("",))[0] != GRAS:
                    continue
                hoog = int(round(h * (math.cos(math.pi * d / r) + 1) / 2))
                if hoog <= 0 or not all(b.leeg(x, y, z) for y in range(f, f + hoog + 3)):
                    continue
                if any(abs(px - x) <= 2 and abs(pz - z) <= 2 for px, _, pz in plekken):
                    continue
                for y in range(f - 1, f - 1 + hoog):
                    b.set(x, y, z, AARDE)
                b.set(x, f - 1 + hoog, z, GRAS)
        b.zone(id, hx - r, f, hz - r, hx + r, f + h, hz + r, wie="een lichte heuvel")


# =====================================================================================================================
# the overlays of the blooming forest and the lit lantern tree
# =====================================================================================================================
def laag_bloei(b):
    """laag_woud_bloei: after the side quests the woud blooms: pink blossoms on the crowns, pink lotus flowers on the
    ground, the dark trees at the rim get light leaves. Only where the build is air."""
    cx, cz, f, r = WOUD["x"], WOUD["z"], WOUD["f"], WOUD["r"]
    cellen = {}
    for (x, y, z), (n, _, _) in list(b.s.blocks.items()):
        if abs(x - cx) > r + 6 or abs(z - cz) > r + 6 or y < f or y > f + 30:
            continue
        if n in (CYAAN, SCHADUW) and _hash(x, y, z, 901) < 0.16:
            for dx, dy, dz in ((0, 1, 0), (1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1)):
                c = (x + dx, y + dy, z + dz)
                if c not in b.s.blocks:
                    cellen[c] = (ROZE if n == CYAAN else CYAAN, {}, None)
                    break
        elif n == GRAS and y == f - 1 and (x, f, z) not in b.s.blocks and (x, f + 1, z) not in b.s.blocks and _hash(x, z, 902) < 0.045:
            cellen[(x, f, z)] = (LOTUSBLAD, {}, None)
    return laag(b, "woud_bloei", cellen)


def laag_lantaarnboom(b):
    """laag_lantaarnboom_licht: the lantern tree all lit: a lantern on every hook and more under the crown."""
    cellen = {}
    for i in range(1, 9):
        x, y, z = b.plekken[f"lantaarnboom_haak_{i}"]["pos"]
        cellen[(x, y, z)] = (LANTAARN, dict(LAMP_HANG), None)
    cx, cz, f = M["x"], M["z"], M["f"]
    laagste = {}
    for (x, y, z), (n, _, _) in b.s.blocks.items():
        if abs(x - cx) <= 8 and abs(z - cz) <= 8 and f + 8 <= y <= f + 26 and n in (CYAAN, ROZE):
            laagste[(x, z)] = min(laagste.get((x, z), 999), y)
    for dx in range(-8, 9):
        for dz in range(-8, 9):
            if (dx * dx + dz * dz) < 9 or (dx + dz) % 3:
                continue
            x, z = cx + dx, cz + dz
            if (x, z) not in laagste:
                continue
            onder = laagste[(x, z)]
            if (x, onder - 1, z) not in b.s.blocks and _hash(x, z, 903) < 0.5:
                cellen[(x, onder - 1, z)] = (LANTAARN, dict(LAMP_HANG), None)
    return laag(b, "lantaarnboom_licht", cellen)
