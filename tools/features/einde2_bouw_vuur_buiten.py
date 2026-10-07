"""
einde2, slice bouw_vuur: what lies around the big crater of the Vuurnatie-hoofdstad.

  grond / na_talud    the pads, paths and stairs of the ground, before it becomes blocks
  haven               the road down from the crater gate, the harbour gate, the quay, the pier with a moored ship, the landing
                      field (aankomst), the team's camp, the beach of the invasion
  kokend_rotsblok     the upper station on the rim, the gondola line with its two cabins, the dock, the prison on the island
  zonnekrijgers       the ruin, the long stair, the terrace of the Drakendans, the two dragon caves, the stair to the arena
  arena               the rock pillars in the crater lake, the dent of the comet shard
  sintelstrand        the beach house and the open-air theatre
  lotus_kamp          the tents of the Witte Lotus
  kade                the quay at the bite in the rim, and leeuwschildpad(): the overlay of the Leeuwschildpad-guh
  natuur              vents, embers, streams of lukewarm lava
  eigen_controle      what the kit's check does not know: nothing that hurts, closed basins, a plain overlay
"""
import math

from features import einde2_bouw_vuur_grond as gr
from features import guheinde_bouw as gb
from features.einde2_bouw_vuur_bouw import (A, AR, A_WATER, BALK, BARST, C, EMBLEEM, F, G, GLAD, GLOEI, GOUD, HAVEN_WATER, HOUT, HOUT_HEK, HOUT_PLAAT, HOUT_TRAP, IJZER,
                                            K, KADE, KOOL, KRV, LAAG, LAVA, LOTUS, MEER, MEER_VLOER, PILAREN, PORTRET, ROOD, ROOD_PLAAT, SAUS, STATION, STEIGER, STENEN,
                                            STENEN_MUUR, STENEN_PLAAT, STENEN_TRAP, TERRAS, TRAP_NOORD, TRAP_Z, ZONNESTEEN, ZWART, ZWART_MUUR, ZWART_PLAAT)
from features.einde2_bouw_vuur_stad import BLAD, RUIT, huis

V = G + 1                           # where you stand on the ground outside the craters
ZAND = "sandstone"
ZAND_GLAD, ZAND_TRAP, ZAND_MUUR, ZAND_BEITEL = "cut_sandstone", "sandstone_stairs", "sandstone_wall", "chiseled_sandstone"
GROT_W = (TERRAS[0] - 18, TERRAS[1])    # the middle of the red dragon's cave (west peak)
GROT_O = (TERRAS[0] + 18, TERRAS[1])    # the blue dragon's (east peak)
BAK = (188, 184, 208, 204)              # the prison: its outer walls
PY = MEER                               # the prison's ground floor (the top of the island)
P2 = PY + 6                             # its upper floor
PDAK = PY + 11                          # its roof


# =====================================================================================================================
# the ground
# =====================================================================================================================
def grond(g, mond):
    """Pads, paths and stairs outside the big crater (called before the basins and before the slope pass)."""
    X, Z = g.X, g.Z
    tx, tz, ty = TERRAS
    # the road: out of the tunnel, six steps down, to the quay
    g.weg([(128, mond + 1, F), (128, 172, F), (128, 178, G), (128, 206, G)], 7)
    # the harbour: quay, the beach of the invasion, the landing field, the camp
    g.vlak(98, 206, 134, 215, G, gr.PLEIN)
    g.vlak(135, 206, 154, 215, G, gr.ZAND)
    g.vlak(100, 185, 118, 203, G, gr.PLEIN)
    g.vlak(83, 185, 99, 203, G, gr.KORST)
    g.weg([(118, 198, G), (125, 198, G)], 5)
    g.weg([(109, 203, G), (109, 207, G)], 5)
    g.vlak(116, 201, 139, 207, G, gr.PLEIN)                 # (the harbour gate)
    # Het Kokend Rotsblok: the stair up the flank to the upper station, the path down the inner wall to the dock
    sx, sz, sy = STATION
    g.vlak(sx - 4, sz - 4, sx + 4, sz + 4, sy, gr.PLEIN)
    voet = sx - 4 - (sy - G)
    for x in range(voet, sx - 4):
        g.vlak(x, sz - 2, x, sz + 2, G + (x - voet), gr.WEG)
    g.weg([(131, 200, G), (voet, sz, G)], 5)
    g.weg([(sx + 2, sz + 5, sy), (sx + 3, sz + 14, sy - 6), (STEIGER[0] - 1, STEIGER[1], MEER)], 3)
    # the Zonnekrijgers: the terrace, the long stair (south), the stair to the arena (north), the ruin
    g.schijf(tx, tz, 9.2, ty, gr.RUINE)
    for z in range(TRAP_Z[1], TRAP_Z[0] + 1):
        g.vlak(tx - 3, z, tx + 3, z, G + (TRAP_Z[0] - z), gr.RUINE)
    for z in range(TRAP_NOORD[0], TRAP_NOORD[1] + 1):
        g.vlak(tx - 2, z, tx + 2, z, min(ty, G + (z - TRAP_NOORD[0])), gr.RUINE)
    g.vlak(16, 152, 52, 182, G, gr.RUINE)
    for cx, cz in (GROT_W, GROT_O):
        g.massief(cx - 10, cz - 10, cx + 12 if cx < tx else cx + 10, cz + 10, ty - 2)
        g.massief(min(cx, tx), cz - 4, max(cx, tx), cz + 4, ty - 2)
    # around the arena: from the stair's foot along the west shore and the north lip to the trail, the causeway to the first
    # pillar, the dent of the comet shard
    nz = TRAP_NOORD[0]
    g.weg([(tx, nz, G), (tx, nz - 3, G)], 5)
    g.weg([(tx, nz - 2, G), (44, 71, G), (A[0] - 2, A[1] + 30, G)], 3)
    g.weg([(A[0] - 2, A[1] + 30, G), (A[0] - 2, A[1] + 26, G + 1), (A[0] - 2, A[1] + 22, G + 2), (A[0] - 2, A[1] + 18, G + 3)], 3)
    g.weg([(tx, nz - 2, G), (27, 56, G), (26, 44, G), (31, 31, G), (42, 22, G), (58, 17, G), (74, 24, G)], 3, maak_land=True)
    g.schijf(23, 50, 3.2, G, gr.ROTS)
    # the trail along the north rim, the quay of the Leeuwschildpad-guh, the way to the camp of the Witte Lotus
    g.weg([(74, 24, G), (86, 14, G), (106, 10, G), (128, 10, G), (150, 10, G), (172, 15, G), (187, 30, G), (197, 50, G), (210, 60, G),
           (KADE[0] - 4, KADE[1], G)], 3, maak_land=True)
    g.weg([(KADE[0], KADE[1] - 3, G), (KADE[0], KADE[1] + 4, G)], 9, maak_land=True)
    g.weg([(KADE[0], KADE[1] + 4, G), (226, 86, G)], 3)
    g.streek(((X - 228) / 21.0) ** 2 + ((Z - 106) / 24.0) ** 2 < 1.0, gr.KORST, G)
    for x0, z0, x1, z1 in ((221, 97, 237, 111), (211, 89, 219, 97), (211, 111, 219, 119), (237, 113, 245, 121)):
        g.vlak(x0, z0, x1, z1, G, gr.KORST)                 # (the tents stand level)
    # the way east of the harbour, over the saddle between the two craters, to the camp
    g.weg([(134, 200, G), (152, 176, G), (172, 158, G + 2), (190, 146, G), (206, 130, G), (216, 120, G)], 5)
    # Sintelstrand: the way from the quay, the beach, the way on to the ruin
    g.weg([(100, 210, G), (84, 208, G)], 5)
    g.streek((X < 97) & (Z > 186 + gr.ruis(g.rng, 10) * 2.0), gr.ZAND, G)
    g.weg([(52, 190, G), (46, 182, G)], 5)


def na_talud(g):
    """After the slope pass: a mound of rock above what is carved out (the two dragon caves and their tunnels)."""
    tx, tz, ty = TERRAS
    for cx, cz in (GROT_W, GROT_O):
        g.koepel(cx, cz, 9, ty + 10)
        g.koepel((cx + (tx - 9 if cx < tx else tx + 9)) / 2.0, cz, 3, ty + 8)


def treden(b, x0, z0, x1, z1, op, name):
    """Stair blocks on a stretch of ground that climbs one block per block towards `op`: every top block of the rectangle
    whose neighbour on the low side lies one lower becomes a stair."""
    g = b.grond
    dx, dz = gb.DIRS[op]
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            y = g.hoogte(x, z)
            if g.hoogte(x - dx, z - dz) == y - 1:
                b.trap(x, y, z, name, op)


# =====================================================================================================================
# the harbour
# =====================================================================================================================
def haven(b):
    g = b.grond
    treden(b, 125, 171, 131, 178, "north", STENEN_TRAP)
    for z in range(180, 204, 8):
        for x in (124, 132):
            b.set(x, g.hoogte(x, z), z, ZWART)
            b.vuurpot(x, g.hoogte(x, z) + 1, z, 2)
    b.sign(123, V, 199, "south", ["Hoofdstad:", "rechtdoor", "Strand: links", "Rotsblok: rechts"], wall=False)
    havenpoort(b)
    kade_en_steiger(b)
    schip(b)
    landingsveld(b)
    kamp(b)
    invasiestrand(b)
    b.prop("uitzicht", 128, 84, 292, wie="in de lucht ten zuiden van de haven: de krater met het paleis erboven")


def havenpoort(b):
    """The harbour gate over the road: two towers, a bridge between them with the emblem, a raised portcullis."""
    z0, z1 = 202, 206
    for x0 in (117, 134):
        x1 = x0 + 4
        b.fill(x0, G, z0, x1, G, z1, ZWART)
        for y in range(V, V + 12):
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    rand = x in (x0, x1) or z in (z0, z1)
                    if rand:
                        b.set(x, y, z, ZWART if (x in (x0, x1) and z in (z0, z1)) or y == V else ROOD)
                    else:
                        b.set(x, y, z, gb.AIR)
        b.fill(x0, V + 12, z0, x1, V + 12, z1, ZWART)
        b.dak(x0 - 1, z0 - 1, x1 + 1, z1 + 1, V + 13)
        xm = x0 + 2
        b.set(xm, V + 7, z1, RUIT)
        b.set(xm, V + 7, z0, RUIT)
        b.set(xm, V + 4, z1, EMBLEEM)
        # the guard's door on the inside of the gate and a lamp in the tower
        xd = x1 if x0 < 128 else x0
        b.lucht(xd, V, 204, xd, V + 1, 204)
        b.deur(xd, V, 204, "west" if x0 < 128 else "east", "iron", open_=True)
        b.hanglamp(xm, V + 12, 204, 3)
        b.set(xm, V, z0 + 1, "guhs:guh_stoel", {"facing": "south"})
    # the bridge and the portcullis
    b.fill(122, V + 7, 203, 133, V + 9, 205, ZWART)
    b.fill(122, V + 8, 204, 133, V + 8, 204, ROOD)
    b.fill(127, V + 8, 206, 129, V + 8, 206, EMBLEEM)
    b.fill(127, V + 8, 202, 129, V + 8, 202, EMBLEEM)
    b.dak(121, 202, 134, 206, V + 10, max_lagen=2, zwier=False)
    for x in range(122, 134):
        b.set(x, V + 6, 204, "iron_bars")
        if x % 2:
            b.set(x, V + 5, 204, "iron_bars")
    for x in range(122, 134):
        b.set(x, G, 204, GLAD)
    b.hanglamp(125, V + 7, 203, 1)
    b.hanglamp(131, V + 7, 205, 1)
    b.sign(121, V + 2, 207, "south", ["Havenpoort", "Alles aangeven:", "knabbels, guhs", "en goede moed"], glow=True)
    b.sign(135, V + 2, 207, "south", ["Vermommingen", "zijn verboden", "(ook hele", "goede), njeg"], glow=True)
    b.prop("havenpoort", 128, G, 204, wie="de drempel van de havenpoort")
    b.plek("poortwacht_1", 124, V, 208, yaw=0.0, wie="poortwacht links", kind="VUURNATIE_MIKA")
    b.plek("poortwacht_2", 132, V, 208, yaw=0.0, wie="poortwacht rechts", kind="VUURNATIE_MIKA")


def kade_en_steiger(b):
    """Bollards and lamps on the quay, crates, the wooden pier into the basin."""
    for x in range(100, 134, 6):
        if abs(x - 128) > 3:
            b.set(x, V, 215, ZWART_MUUR)
            b.lantaarn(x, V + 1, 215, False)
    for x, z, hoog in ((103, 208, 2), (104, 208, 1), (103, 209, 1), (120, 212, 1), (121, 212, 2), (121, 213, 1)):
        for k in range(hoog):
            b.ton(x, V + k, z)
    b.sign(105, V, 209, "east", ["Vracht voor de", "Vuurheer: thee,", "thee, kussens,", "nog meer thee"], wall=False)
    vloer = b.grond.hoogte(128, 220)
    for z in range(216, 238):
        for x in (127, 128, 129):
            b.set(x, G, z, HOUT if z % 4 else BALK, None if z % 4 else {"axis": "x"})
        if z % 5 == 1:
            for x in (127, 129):
                b.fill(x, vloer + 1, z, x, G - 1, z, BALK, {"axis": "y"})
    for z in (220, 228, 236):
        b.fill(126, vloer + 1, z, 126, G, z, BALK, {"axis": "y"})
        b.set(126, V, z, HOUT_HEK)
        b.lantaarn(126, V + 1, z, False)
    b.sign(128, V, 237, "north", ["Einde steiger", "Verder zwemmen", "op eigen", "risico, njeg"], wall=False)
    b.geen_licht.update((x, z) for x in (126, 127, 128, 129) for z in range(216, 238))


def schip(b):
    """A small iron ship of the Vuurnatie moored at the pier: a pointed bow to the south, a deckhouse, a smoking funnel."""
    x0, x1, z0, z1 = 131, 137, 219, 238
    dek = G + 1
    for z in range(z0, z1 + 1):
        smal = max(0, z - (z1 - 3))
        for x in range(x0 + smal, x1 - smal + 1):
            rand = x in (x0 + smal, x1 - smal) or z in (z0, z1)
            for y in range(G - 3, dek + 1):
                if rand or y in (G - 3, dek):
                    b.set(x, y, z, IJZER if (x + y + z) % 3 else GLAD)
                else:
                    b.set(x, y, z, gb.AIR)
            if rand:
                b.set(x, dek + 1, z, ZWART_MUUR)
    b.set(134, dek + 1, z1, EMBLEEM)
    # the deckhouse at the stern, the funnel
    b.fill(132, dek + 1, z0 + 1, 136, dek + 3, z0 + 5, ROOD)
    b.lucht(133, dek + 1, z0 + 2, 135, dek + 2, z0 + 4)
    b.fill(132, dek + 4, z0 + 1, 136, dek + 4, z0 + 5, ZWART_PLAAT, gb.slab())
    b.lucht(134, dek + 1, z0 + 5, 134, dek + 2, z0 + 5)
    b.deur(134, dek + 1, z0 + 5, "north", "iron", open_=True)
    b.set(133, dek + 2, z0 + 5, RUIT)
    b.set(135, dek + 2, z0 + 5, RUIT)
    b.lantaarn(134, dek + 3, z0 + 3, True)
    b.set(133, dek + 1, z0 + 2, "guhs:guh_stoel", {"facing": "south"})
    b.fill(134, dek + 4, z0 + 2, 134, dek + 7, z0 + 2, ZWART_MUUR)
    b.set(134, dek + 8, z0 + 2, "guhs:rookgat")
    # the gangway from the pier, a lamp at the bow
    b.set(130, G, 226, HOUT)
    b.set(130, V, 226, HOUT_PLAAT, gb.slab())
    b.set(x0, dek + 1, 226, gb.AIR)
    b.lantaarn(134, dek + 2, z1, False)
    b.sign(x0, dek + 2, 224, "west", ["VNS Aangebrand", "Vertrekt nooit", "op tijd", "(wel met rook)"])
    b.geen_licht.update((x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1))
    b.zone("schip", x0, G - 3, z0, x1, dek + 9, z1, wie="het afgemeerde Vuurnatie-schip VNS Aangebrand")


def landingsveld(b):
    """Where you arrive: a paved field with a sun in it, a brazier on every corner."""
    cx, cz = 109, 194
    for x in range(100, 119):
        for z in range(185, 204):
            d = math.hypot(x - cx, z - cz)
            if d < 1.2 or (abs(d - 4) < 0.6 and (x == cx or z == cz)):
                b.set(x, G, z, GLOEI)
            elif abs(d - 7) < 0.6 or (d < 7 and (x == cx or z == cz)):
                b.set(x, G, z, ROOD)
    for x, z in ((100, 185), (118, 185), (100, 203), (118, 203)):
        b.set(x, G, z, ZWART)
        b.vuurpot(x, V, z, 2)
    b.zone("aankomst", 101, V, 186, 117, V + 8, 202, wie="het landingsveld bij de haven")
    b.prop("aankomst_speler", cx, V, cz, wie="waar je staat als je bent aangekomen")
    b.prop("rustpunt_1", cx, V, cz + 6, wie="rustpunt: het landingsveld")
    b.sign(119, V, 189, "west", ["Landingsveld", "Appa's: vak A", "Vetbootjes: B", "Draken: nee"], wall=False)
    b.geen_licht.update((x, z) for x in range(101, 118) for z in range(186, 203))


def kamp(b):
    """The team's camp next to the landing field: a lukewarm fire with logs around it, a lean-to, hay for Appa."""
    fx, fz = 93, 191
    b.set(fx, G, fz, GLOEI)
    b.vlam(fx, V, fz)
    for dx, dz, kant in ((-2, 0, "east"), (2, 0, "west"), (0, -2, "south"), (0, 2, "north")):
        b.stoel(fx + dx, V, fz + dz, kant)
    # the lean-to against the north side
    for x in range(85, 90):
        b.set(x, V, 186, "red_wool")
        b.set(x, V + 1, 186, "red_wool")
        b.set(x, V + 2, 186, "orange_wool")
        b.set(x, V + 2, 187, "orange_wool")
        b.plaat(x, V + 2, 188, HOUT_PLAAT)
    for x in (85, 89):
        b.fill(x, V, 188, x, V + 1, 188, HOUT_HEK)
    b.set(86, V, 187, "guhs:guh_slaapzak", {"facing": "east", "occupied": "false"})
    b.set(88, V, 187, "guhs:guh_slaapzak", {"facing": "east", "occupied": "false"})
    b.lantaarn(87, V + 1, 187, True)
    # Appa's corner: hay, a water trough; five by five stays free
    for x, z, hoog in ((84, 201, 2), (85, 202, 1), (84, 202, 2), (84, 196, 1)):
        for k in range(hoog):
            b.set(x, V + k, z, "hay_block", {"axis": "y"})
    b.set(92, V, 201, "water_cauldron", {"level": "3"})
    b.sign(91, V, 202, "north", ["Kamp Guhvatar", "Niet voeren:", "Momo (al vol)", "njeg"], wall=False)
    b.zone("kamp", 83, V, 185, 99, V + 7, 203, wie="het kamp van het team naast het landingsveld")
    b.prop("appa_kamp", 88, V, 197, wie="waar Appa-guh ligt (vijf bij vijf vrij)")
    for id, dx, dz, yaw, wie in (("team_aang", -1, -2, 0.0, "Aang-guh"), ("team_momo", 1, -2, 0.0, "Momo-guh"), ("team_katara", 3, -1, 90.0, "Katara-guh"),
                                 ("team_sokka", 3, 1, 90.0, "Sokka-guh"), ("team_toph", -3, 1, -90.0, "Toph-guh"), ("team_zuka", 1, 3, 180.0, "Zuka")):
        b.plek(id, fx + dx, V, fz + dz, yaw=yaw, wie=wie + " bij het kampvuur")
    b.geen_licht.update((x, z) for x in range(86, 91) for z in range(195, 200))


def invasiestrand(b):
    """The beach east of the pier where the invasion lands: barricades, a watch tower, a sign that forbids it."""
    for x, z in ((138, 211), (143, 213), (148, 210)):
        for k in (-1, 0, 1):
            b.set(x + k, V, z, HOUT_HEK)
        b.set(x, V + 1, z, HOUT_HEK)
    # the watch tower
    x0, z0 = 150, 206
    for x in (x0, x0 + 3):
        for z in (z0, z0 + 3):
            b.fill(x, V, z, x, V + 6, z, BALK, {"axis": "y"})
    b.fill(x0, V + 7, z0, x0 + 3, V + 7, z0 + 3, HOUT)
    for x in range(x0, x0 + 4):
        for z in range(z0, z0 + 4):
            if x in (x0, x0 + 3) or z in (z0, z0 + 3):
                b.set(x, V + 8, z, HOUT_HEK)
    for y in range(V, V + 8):
        b.set(x0 + 1, y, z0 + 1, "ladder", {"facing": "south", "waterlogged": "false"})
    b.fill(x0 + 1, V, z0, x0 + 1, V + 6, z0, HOUT)
    b.set(x0 + 1, V + 7, z0 + 1, "ladder", {"facing": "south", "waterlogged": "false"})
    b.set(x0 + 1, V + 7, z0, HOUT)
    b.lantaarn(x0 + 2, V + 8, z0 + 2, False)
    b.sign(140, V, 207, "south", ["Verboden te", "invaderen", "(vooral op de", "Dag v.d. Zon)"], wall=False)
    b.zone("invasie_strand", 135, G - 4, 204, 154, V + 9, 228, wie="het strand waar de invasie aan land gaat")
    b.plek("strandwacht", x0 + 2, V + 8, z0 + 1, yaw=0.0, wie="de uitkijk op de wachttoren", kind="VUURNATIE_MIKA")


# =====================================================================================================================
# Het Kokend Rotsblok
# =====================================================================================================================
def kokend_rotsblok(b):
    g = b.grond
    sx, sz, sy = STATION
    voet = sx - 4 - (sy - G)
    treden(b, voet + 1, sz - 1, sx - 4, sz + 1, "east", STENEN_TRAP)
    for x in range(voet, sx - 4):
        for z in (sz - 2, sz + 2):
            y = g.hoogte(x, z)
            b.set(x, y, z, STENEN)
            b.set(x, y + 1, z, STENEN_MUUR if x % 4 else GLOEI)
            if x % 4 == 0:
                b.vlam(x, y + 2, z)
    b.sign(voet - 1, V, sz - 3, "west", ["Het Kokend", "Rotsblok", "Bezoekuur:", "nooit, njeg"], wall=False)
    bovenstation(b)
    gondellijn(b)
    steiger(b)
    gevangenis(b)
    b.zone("kokend_rotsblok", K[0] - 44, G - 4, K[1] - 44, K[0] + 44, PDAK + 8, K[1] + 44, wie="de krater van Het Kokend Rotsblok")
    b.zone("kokend_meer", K[0] - KRV, MEER_VLOER, K[1] - KRV, K[0] + KRV, MEER + 1, K[1] + KRV, wie="het meer van kokende kaassaus")


def bovenstation(b):
    """The upper station on the rim: a roof on four pillars, the winch, a waiting bench."""
    sx, sz, sy = STATION
    v = sy + 1
    for x in range(sx - 4, sx + 5):
        for z in range(sz - 4, sz + 5):
            b.set(x, sy, z, GLAD if (x + z) % 2 else STENEN)
    for x in (sx - 3, sx + 3):
        for z in (sz - 3, sz + 3):
            b.fill(x, v, z, x, v + 4, z, ZWART_MUUR)
            b.set(x, v, z, ZWART)
    b.fill(sx - 3, v + 5, sz - 3, sx + 3, v + 5, sz + 3, HOUT)
    b.dak(sx - 4, sz - 4, sx + 4, sz + 4, v + 5, max_lagen=3)
    b.hanglamp(sx - 2, v + 5, sz, 1)
    b.hanglamp(sx + 2, v + 5, sz - 2, 1)
    # the winch: a drum of grill iron on two blocks, a lever
    b.set(sx - 1, v, sz - 2, ZWART)
    b.set(sx - 1, v, sz + 2, ZWART)
    for z in range(sz - 2, sz + 3):
        b.set(sx - 1, v + 1, z, "guhs:roosterijzer_pilaar", {"axis": "z"})
    b.hendel(sx, v, sz + 2, "east", "floor")
    b.set(sx - 3, v, sz - 1, "guhs:guh_bank", {"facing": "east"})
    b.sign(sx + 1, v, sz - 3, "south", ["Gondel naar", "het Rotsblok", "Enkele reis", "(retour: lol)"], wall=False)
    b.prop("rustpunt_5", sx - 2, v, sz + 1, wie="rustpunt: het bovenstation van de gondel")
    b.plek("gondelwacht", sx + 1, v, sz + 2, yaw=90.0, wie="de bewaker bij de lier", kind="VUURNATIE_MIKA")
    b.zone("bovenstation", sx - 4, v, sz - 4, sx + 4, v + 8, sz + 4, wie="het bovenstation van de gondel")


def cabine(b, x0, y, z, open_kant):
    """A gondola cabin of 3 x 3: an iron floor at y, bars around (open on one side), a roof and the hanger above it."""
    for x in range(x0, x0 + 3):
        for dz in (-1, 0, 1):
            b.set(x, y, z + dz, IJZER)
            b.set(x, y + 3, z + dz, GLAD)
            rand = x in (x0, x0 + 2) or dz
            open_ = (open_kant == "west" and x == x0 and dz == 0) or (open_kant == "east" and x == x0 + 2 and dz == 0)
            if rand and not open_ and not (x == x0 + 1 and dz == 0):
                b.set(x, y + 1, z + dz, "iron_bars")
                b.set(x, y + 2, z + dz, "iron_bars")
    b.lantaarn(x0 + 1, y + 2, z, True)


def gondellijn(b):
    """The line from the upper station down to the prison's roof: two masts, the cable (chain), a cabin at each end."""
    sx, sz, sy = STATION
    x0, _, x1, _ = BAK
    mast_a, mast_b = sx + 4, x0 + 2
    ya, yb = sy + 8, PDAK + 6
    # the gantry at the upper station (two posts and a beam, the cabin passes between them), the mast on the prison's roof
    for z in (sz - 2, sz + 2):
        b.fill(mast_a, sy + 1, z, mast_a, ya, z, ZWART_MUUR)
    b.fill(mast_a, ya, sz - 2, mast_a, ya, sz + 2, ZWART_MUUR)
    b.fill(mast_b, PDAK + 5, sz, mast_b, yb, sz, ZWART_MUUR)
    # the cable: bars, stepping down twice on the way (a cable of chain would hang on nothing: a chain hangs from above)
    y = ya
    for x in range(mast_a + 1, mast_b):
        if x in (mast_a + 9, mast_a + 17):
            b.set(x, y, sz, "iron_bars")
            y -= 1
        b.set(x, y, sz, "iron_bars")
    assert y == yb, (y, yb)
    # cabin A at the upper station (you step in from the platform), cabin B at the prison (you step out onto the roof)
    ax = mast_a + 1
    cabine(b, ax, sy, sz, "west")
    b.fill(ax + 1, sy + 4, sz, ax + 1, ya - 1, sz, "iron_bars")
    bx = x0 - 3
    cabine(b, bx, PDAK, sz, "east")
    b.fill(bx + 1, PDAK + 4, sz, bx + 1, yb - 1, sz, "iron_bars")
    b.prop("gondel_boven", ax + 1, sy + 1, sz, wie="in de gondel bij het bovenstation (op de kraterrand)")
    b.prop("gondel_onder", bx + 1, PDAK + 1, sz, wie="in de gondel bij het dak van de gevangenis")
    b.pad("gondel_route", [(ax + 1, sy + 1, sz), ((ax + bx) // 2 + 1, sy - 1, sz), (bx + 1, PDAK + 1, sz)], wie="de gondellijn over het kokende meer")
    b.prop("azula_gondel", bx + 1, PDAK + 4, sz, wie="boven op de gondel: waar Azula-Mika staat bij de achtervolging", los=True)


def steiger(b):
    """The little dock at the foot of the inner wall, where the guards' rowing boat lies in the sauce."""
    x, z = STEIGER
    for dx, dz in ((2, -1), (2, 0), (2, 1), (3, -1), (3, 1), (4, -1), (4, 0), (4, 1)):
        b.set(x + dx + 1, MEER, z + dz, HOUT_PLAAT if (dx, dz) not in ((2, 0), (4, 0)) else HOUT_TRAP,
              gb.slab() if (dx, dz) not in ((2, 0), (4, 0)) else gb.stair("east" if dx == 2 else "west"))
    b.set(x, MEER + 1, z - 2, ZWART_MUUR)
    b.lantaarn(x, MEER + 2, z - 2, False)
    b.sign(x - 1, MEER + 1, z + 2, "east", ["Roeiboot van de", "bewakers. Niet", "lenen. Roeit", "zwaar in saus"], wall=False)
    b.prop("steiger", x, MEER + 1, z, wie="het steigertje onder aan de binnenwand (de sluipweg over het meer)")


def gevangenis(b):
    """The prison on the island: four wings around a yard, two floors and a roof.
    Ground floor: west the entry hall and the stair, north and south three cells each, east the koelcel.
    Upper floor: west the guard room with the control desk at the window, north the director, east the barracks, south the
    store; a gallery runs around the yard. On the roof the lower station of the gondola."""
    x0, z0, x1, z1 = BAK
    wand = [(IJZER, 46), (STENEN, 34), (BARST, 10), (GLAD, 10)]
    v1, v2, vd = PY + 1, P2 + 1, PDAK + 1
    yx0, yz0, yx1, yz1 = x0 + 5, z0 + 5, x1 - 5, z1 - 5          # the yard (inside)
    # --- the shell: outer walls, the walls around the yard, floors, roof ---
    b.fill(x0, PY, z0, x1, PY, z1, STENEN)
    for y in range(v1, PDAK + 1):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                buiten = x in (x0, x1) or z in (z0, z1)
                binnen = (x in (yx0 - 1, yx1 + 1) and yz0 - 1 <= z <= yz1 + 1) or (z in (yz0 - 1, yz1 + 1) and yx0 - 1 <= x <= yx1 + 1)
                in_plein = yx0 <= x <= yx1 and yz0 <= z <= yz1
                # (the wings run through to the corners: west and east over the full length)
                tussen = (x in (yx0 - 1, yx1 + 1)) and not (yz0 - 1 <= z <= yz1 + 1)
                if buiten or binnen or tussen:
                    b.set(x, y, z, b.kies(wand))
                elif in_plein:
                    b.set(x, y, z, gb.AIR)
                elif y in (P2, PDAK):
                    b.set(x, y, z, STENEN if y == P2 else GLAD)
                else:
                    b.set(x, y, z, gb.AIR)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x in (x0, x1) or z in (z0, z1)) and (x + z) % 2 == 0:
                b.set(x, vd, z, ZWART_MUUR)
            elif x in (x0, x1) or z in (z0, z1):
                b.set(x, vd, z, STENEN_PLAAT, gb.slab())
    for x in (yx0 - 1, yx1 + 1):
        for z in range(yz0 - 1, yz1 + 2):
            b.set(x, vd, z, "iron_bars")
    for z in (yz0 - 1, yz1 + 1):
        for x in range(yx0 - 1, yx1 + 2):
            b.set(x, vd, z, "iron_bars")
    # --- the yard: paving, a brazier in the middle, the gallery of the upper floor (two wide: a walk and a rail) ---
    for x in range(yx0, yx1 + 1):
        for z in range(yz0, yz1 + 1):
            b.set(x, PY, z, STENEN if (x + z) % 2 else BARST)
            ring = min(x - yx0, yx1 - x, z - yz0, yz1 - z)
            if ring == 0:
                b.set(x, P2, z, STENEN_PLAAT, gb.slab("top"))
            elif ring == 1:
                b.set(x, v2, z, "iron_bars")
                b.set(x, P2, z, STENEN_PLAAT, gb.slab("top"))
    cx, cz = (yx0 + yx1) // 2, (yz0 + yz1) // 2
    b.set(cx, PY, cz, GLOEI)
    b.vuurpot(cx, v1, cz, 1)
    b.geen_licht.add((cx, cz))
    for x, z in ((yx0, yz0), (yx1, yz0), (yx0, yz1), (yx1, yz1)):
        b.lantaarn(x, P2 - 1, z, True)
    b.zone("gevangenis_plein", yx0, v1, yz0, yx1, PDAK, yz1, wie="de binnenplaats van de gevangenis")
    b.sign(cx, v1, cz + 2, "south", ["Luchten:", "1x per dag", "Ontsnappen:", "0x per dag"], wall=False)
    # --- ground floor, west: the entry hall, the gate to the dock, the door to the yard, the stair up ---
    zm = (z0 + z1) // 2
    b.lucht(x0, v1, zm, x0, v1 + 1, zm)
    b.deur(x0, v1, zm, "east", "iron", open_=True)
    b.lucht(yx0 - 1, v1, zm, yx0 - 1, v1 + 1, zm)
    b.deur(yx0 - 1, v1, zm, "east", "iron", open_=True)
    for k in range(6):
        z = zm - 3 - k
        b.trap(x0 + 1, PY + 1 + k, z, STENEN_TRAP, "north")
        if k:
            b.fill(x0 + 1, v1, z, x0 + 1, PY + k, z, STENEN)
        b.lucht(x0 + 1, PY + 2 + k, z, x0 + 1, P2 + 3, z)
    for z in range(zm - 8, zm - 2):
        b.set(x0 + 2, v2, z, "iron_bars")
    b.set(x0 + 2, v2, zm - 8, gb.AIR)
    b.set(x0 + 1, v1, z1 - 2, "guhs:guh_tafel", {"facing": "north"})
    b.set(x0 + 1, v1, z1 - 3, "guhs:guh_stoel", {"facing": "south"})
    b.sign(x0 + 3, v1 + 1, zm + 2, "west", ["Balie", "Naam, misdaad", "en schoenmaat", "invullen a.u.b."])
    for z in (z0 + 3, zm + 4, z1 - 2):
        b.lantaarn(x0 + 2, P2 - 1, z, True)
    b.prop("rustpunt_6", x0 + 2, v1, zm + 3, wie="rustpunt: de hal van de gevangenis")
    b.prop("gevangenis_poort", x0, v1, zm, wie="de poort van de gevangenis aan de steigerkant")
    # the landing outside the gate: planks on the shore
    for x in range(x0 - 4, x0):
        for z in range(zm - 1, zm + 2):
            if b.naam(x, PY, z) in (None, gb.AIR, gb.mc(SAUS)) or b.naam(x, PY, z) == SAUS:
                b.set(x, PY, z, HOUT)
                b.set(x, PY - 1, z, BALK, {"axis": "y"})
    # --- ground floor, north and south: three cells each, an iron door to the yard ---
    cel = 0
    for zc, deur_z, kant in ((z0 + 1, yz0 - 1, "south"), (z1 - 3, yz1 + 1, "north")):
        for k in range(3):
            cx0 = yx0 + 4 * k
            if k:
                b.fill(cx0 - 1, v1, zc, cx0 - 1, P2 - 1, zc + 2, IJZER)
            xd = cx0 + 1
            b.lucht(xd, v1, deur_z, xd, v1 + 1, deur_z)
            b.deur(xd, v1, deur_z, kant, "iron")
            b.set(xd - 1, v1 + 1, deur_z, "iron_bars")
            b.set(xd + 1, v1 + 1, deur_z, "iron_bars")
            achter = zc if kant == "south" else zc + 2
            b.set(cx0, v1, achter, "guhs:guh_slaapzak", {"facing": "east", "occupied": "false"})
            b.set(cx0 + 2, v1, achter, "cauldron")
            b.lantaarn(xd, P2 - 1, zc + 1, True)
            cel += 1
            midden = (xd, v1, zc + 1)
            if kant == "south":
                b.prop(f"knipoog_stitch_celdeur_{k + 1}", xd, v1, deur_z, wie="een celdeur die openvliegt (onderste helft)")
                b.plek(f"gevangene_{k + 1}", *midden, yaw=0.0, wie="een gevangen guh in zijn cel", kind="GEVANGENE_GUH", los=True)
            elif k == 2:
                b.plek("suki", *midden, yaw=180.0, wie="Suki-guh in haar cel", kind="SUKI_GUH", los=True)
                b.prop("cel_suki", xd, v1, deur_z, wie="de deur van Suki-guh's cel (onderste helft)")
                b.sign(xd + 1, v1 + 2, deur_z - 1, "north", ["Cel 6: krijgster", "Niet aaien,", "bijt terug", "(duwt terug)"])
            else:
                b.plek(f"gevangene_{4 + k}", *midden, yaw=180.0, wie="een gevangen guh in zijn cel", kind="GEVANGENE_GUH", los=True)
    # --- ground floor, east: the koelcel (three booths of ice behind iron doors) ---
    ex0, ex1 = yx1 + 2, x1 - 1
    b.lucht(yx1 + 1, v1, zm, yx1 + 1, v1 + 1, zm)
    b.deur(yx1 + 1, v1, zm, "west", "iron", open_=True)
    for z in range(z0 + 1, z1):
        b.set(ex1, v1, z, "packed_ice")
        b.set(ex1, v1 + 1, z, "packed_ice" if z % 2 else "blue_ice")
        b.set(ex1, v1 + 2, z, "packed_ice")
    for k, z in enumerate((zm - 6, zm - 3, zm + 5)):
        b.fill(ex0 + 1, v1, z - 1, ex1, P2 - 1, z + 1, "packed_ice")
        b.set(ex1, v1 + 2, z, "sea_lantern")
        b.lucht(ex0 + 2, v1, z, ex0 + 2, v1 + 1, z)
        b.set(ex0 + 2, PY, z, "snow_block")
        b.lucht(ex0 + 1, v1, z, ex0 + 1, v1 + 1, z)
        b.deur(ex0 + 1, v1, z, "west", "iron")
        if k == 1:
            b.prop("koelcel", ex0 + 1, v1, z, wie="de deur van de middelste koelcel (onderste helft)")
    b.sign(ex0, v1, zm + 2, "north", ["De Koelcel", "Voor wie te", "heet gebakerd", "is, njeg"], wall=False)
    for z in (z0 + 2, zm + 2, z1 - 2):
        b.lantaarn(ex0, P2 - 1, z, True)
    # --- upper floor, west: the guard room, its window on the yard and the control desk ---
    for z in range(yz0 + 1, yz1):
        b.set(yx0 - 1, v2 + 1, z, "glass_pane")
        b.set(yx0 - 1, v2 + 2, z, "glass_pane")
    for z in (zm - 1, zm, zm + 1):
        b.set(yx0 - 2, v2, z, GLAD)
        b.set(yx0 - 3, v2, z, "polished_blackstone_button" if z != zm else "lever",
              {"face": "wall", "facing": "west", "powered": "false"})
    b.prop("knipoog_stitch_paneel", yx0 - 2, v2, zm, wie="het bedieningspaneel aan het raam (626-guh zit erop)")
    b.plek("knipoog_stitch_bewaker", yx0 - 3, v2, zm + 3, yaw=180.0, wie="de bewaker-Mika die het niet snapt", kind="VUURNATIE_MIKA")
    b.camera("knipoog_stitch", (yx1 - 2, v2 + 2, yz1 - 2), (yx0 - 2, v2 + 1, zm), zicht=["knipoog_stitch_paneel", "knipoog_stitch_celdeur_1"])
    b.sign(x0 + 1, v2 + 1, zm + 2, "east", ["Knop 1: deuren", "Knop 2: licht", "Knop 3: nooit", "indrukken, njeg"])
    b.set(x0 + 1, v2, z1 - 1, "guhs:guh_stoel", {"facing": "north"})
    b.kist(x0 + 1, v2, z1 - 2, "east", "gevangenis")
    for z in (z0 + 2, zm + 4, z1 - 2):
        b.lantaarn(x0 + 2, PDAK - 1, z, True)
    # the door from the guard room onto the gallery
    b.lucht(yx0 - 1, v2, yz0, yx0 - 1, v2 + 1, yz0)
    b.deur(yx0 - 1, v2, yz0, "east", "iron", open_=True)
    b.zone("wachtkamer", x0 + 1, v2, z0 + 1, yx0 - 2, PDAK - 1, z1 - 1, wie="de wachtkamer met uitzicht op de binnenplaats")
    # --- upper floor, north: the director ---
    b.lucht(cx, v2, yz0 - 1, cx, v2 + 1, yz0 - 1)
    b.deur(cx, v2, yz0 - 1, "south", "mangrove")
    b.set(cx - 2, v2, z0 + 1, HOUT_PLAAT, gb.slab("top"))
    b.set(cx - 1, v2, z0 + 1, HOUT_PLAAT, gb.slab("top"))
    b.set(cx - 2, v2 + 1, z0 + 1, "guhs:theepotje", {"facing": "south"})
    b.set(cx + 4, v2, z0 + 1, "guhs:potted_knabbelroos")
    b.set(yx0, v2 + 1, z0, PORTRET)
    b.tapijt(yx0 + 1, v2, z0 + 2, yx1 - 1, z0 + 3, "red")
    b.set(cx - 1, v2, z0 + 2, gb.AIR)
    b.sign(cx + 1, v2 + 1, yz0 - 2, "north", ["Directeur", "Niemand is hier", "ooit ontsnapt", "(tot vandaag)"])
    b.plek("directeur", cx - 1, v2, z0 + 2, yaw=0.0, wie="de gevangenisdirecteur achter zijn bureau", kind="GEVANGENIS_DIRECTEUR_MIKA")
    b.lantaarn(cx + 2, PDAK - 1, z0 + 2, True)
    b.lantaarn(cx - 3, PDAK - 1, z0 + 2, True)
    # --- upper floor, east: the barracks; south: the store ---
    b.lucht(yx1 + 1, v2, zm, yx1 + 1, v2 + 1, zm)
    b.deur(yx1 + 1, v2, zm, "west", "mangrove")
    for z in range(z0 + 1, z1 - 1, 3):
        if abs(z - zm) > 1:
            b.slaapzak(ex1, v2, z, "east")
    for z in (z0 + 2, zm + 3, z1 - 2):
        b.lantaarn(ex0, PDAK - 1, z, True)
    b.sign(ex0, v2, zm + 2, "north", ["Slaapzaal", "Snurken mag", "Ontsnappen", "melden"], wall=False)
    b.lucht(cx, v2, yz1 + 1, cx, v2 + 1, yz1 + 1)
    b.deur(cx, v2, yz1 + 1, "north", "mangrove")
    for x in range(yx0, yx1 + 1, 2):
        b.ton(x, v2, z1 - 1)
        if x % 4 == 0:
            b.ton(x, v2 + 1, z1 - 1)
    b.lantaarn(cx, PDAK - 1, z1 - 2, True)
    b.lantaarn(yx0 + 1, PDAK - 1, z1 - 2, True)
    b.lantaarn(yx1 - 1, PDAK - 1, z1 - 2, True)
    # --- the ladder from the guard room to the roof, the lower station ---
    for y in range(v2, PDAK + 1):
        b.set(x0 + 1, y, z1 - 5, "ladder", {"facing": "east", "waterlogged": "false"})
    sz = STATION[1]
    for z in (sz - 1, sz, sz + 1):
        b.set(x0, vd, z, gb.AIR)
    for x in (x0 + 1, x0 + 4):
        for z in (sz - 2, sz + 2):
            b.fill(x, vd, z, x, vd + 2, z, ZWART_MUUR)
    b.fill(x0 + 1, vd + 3, sz - 2, x0 + 4, vd + 3, sz + 2, ROOD_PLAAT, gb.slab())
    b.lantaarn(x0 + 3, vd + 2, sz, True)
    b.plek("ty_lee", x0 + 2, vd, sz + 1, yaw=90.0, wie="Ty Lee-Mika bij het benedenstation", kind="TY_LEE_MIKA")
    for x, z in ((x0 + 6, z0 + 2), (x1 - 2, z0 + 2), (x1 - 2, z1 - 2), (x0 + 6, z1 - 2), (x1 - 2, zm)):
        b.set(x, PDAK, z, GLOEI)
    for tx_, tz_ in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
        ax, az = (x0 if tx_ == x0 else x1 - 2), (z0 if tz_ == z0 else z1 - 2)
        b.fill(ax, vd, az, ax + 2, vd + 3, az + 2, ZWART)
        b.fill(ax, vd + 2, az, ax + 2, vd + 2, az + 2, ROOD)
        b.set(ax + 1, vd + 2, az + 1, GLOEI)
        b.dak(ax - 1, az - 1, ax + 3, az + 3, vd + 4, zwier=False)
    for k in range(x0 + 3, x1 - 2, 3):
        for z in (z0, z1):
            b.set(k, v2 + 1, z, "iron_bars")
    for k in range(z0 + 3, z1 - 2, 3):
        b.set(x1, v2 + 1, k, "iron_bars")
        if abs(k - zm) > 2:
            b.set(x0, v2 + 1, k, "iron_bars")
    b.fill(x0, v1 + 2, zm - 1, x0, v1 + 2, zm + 1, ROOD)
    b.set(x0, v1 + 3, zm, EMBLEEM)
    b.sign(x0 - 1, v1 + 2, zm, "west", ["Het Kokend", "Rotsblok", "Uitchecken kan", "niet, njeg"], glow=True)
    b.zone("gevangenis", x0, v1, z0, x1, vd + 6, z1, wie="de gevangenis")
    b.pad("inbraak_route", [(x0 - 2, vd, sz), (x0 + 2, vd, sz), (x0 + 1, vd, z1 - 5), (x0 + 1, v2, z1 - 5), (x0 + 2, v2, zm - 9), (x0 + 1, v2, zm - 9), (x0 + 1, v1, zm - 2),
                            (x0 + 2, v1, zm), (yx0, v1, zm), (yx1 - 1, v1, yz1)], wie="van de gondel over het dak naar binnen, tot Suki-guh's cel")


# =====================================================================================================================
# the Zonnekrijgers
# =====================================================================================================================
def zonnekrijgers(b):
    g = b.grond
    tx, tz, ty = TERRAS
    # --- the long stair: sandstone steps between balustrades, a flame every sixth step ---
    treden(b, tx - 2, TRAP_Z[1], tx + 2, TRAP_Z[0] - 1, "north", ZAND_TRAP)
    for z in range(TRAP_Z[1], TRAP_Z[0] + 1):
        for x in (tx - 3, tx + 3):
            y = g.hoogte(x, z)
            b.set(x, y, z, ZAND_GLAD)
            if z % 6 == 0:
                b.set(x, y + 1, z, GLOEI)
                b.vlam(x, y + 2, z)
            else:
                b.set(x, y + 1, z, ZAND_MUUR)
    treden(b, tx - 1, TRAP_NOORD[0] + 1, tx + 1, TRAP_NOORD[1], "south", ZAND_TRAP)
    for z in range(TRAP_NOORD[0], TRAP_NOORD[1] + 1):
        for x in (tx - 2, tx + 2):
            y = g.hoogte(x, z)
            b.set(x, y, z, ZAND_GLAD)
            if z % 6 == 0:
                b.set(x, y + 1, z, GLOEI)
                b.vlam(x, y + 2, z)
            else:
                b.set(x, y + 1, z, ZAND_MUUR)
    b.prop("trap_onder", tx, V, TRAP_Z[0] + 1, wie="de voet van de lange trap")
    b.prop("trap_boven", tx, ty + 1, TRAP_Z[1] - 1, wie="boven aan de lange trap, op het terras")
    b.prop("trap_arena", tx, V, TRAP_NOORD[0] - 1, wie="de voet van de trap naar de duelarena")
    terras(b)
    drakengrot(b, GROT_W, "rood")
    drakengrot(b, GROT_O, "blauw")
    ruine(b)
    b.zone("zonnekrijger_ruine", 8, G - 2, TRAP_NOORD[0], 62, 112, 184, wie="de ruïne van de Zonnekrijgers, met de trap en de drakengrotten")


def terras(b):
    """The round terrace of the Drakendans: a sun in the floor, eight columns with flames, the two mirrored step routes."""
    tx, tz, ty = TERRAS
    for x in range(tx - 9, tx + 10):
        for z in range(tz - 9, tz + 10):
            d = math.hypot(x - tx, z - tz)
            if d > 9.2:
                continue
            hoek = math.degrees(math.atan2(z - tz, x - tx)) % 45
            if d < 1.5:
                b.set(x, ty, z, LAVA)
            elif abs(d - 5) < 0.5 and min(hoek, 45 - hoek) < 5.5:
                b.set(x, ty, z, GLOEI)
            elif d < 3.4 or abs(d - 7) < 0.55 or (d < 7 and min(hoek, 45 - hoek) < 5.5):
                b.set(x, ty, z, ZONNESTEEN)
            else:
                b.set(x, ty, z, ZAND_GLAD if (x + z) % 2 else "smooth_sandstone")
    for k in range(8):
        a = math.radians(22.5 + 45 * k)
        x, z = int(round(tx + 8.2 * math.cos(a))), int(round(tz + 8.2 * math.sin(a)))
        b.set(x, ty, z, ZAND_GLAD)
        b.fill(x, ty + 1, z, x, ty + 3, z, ZAND_MUUR)
        b.set(x, ty + 4, z, ZAND_BEITEL)
        b.set(x, ty + 5, z, GLOEI)
        b.vlam(x, ty + 6, z)
        b.geen_licht.add((x, z))
    b.zone("drakendans", tx - 9, ty + 1, tz - 9, tx + 9, ty + 8, tz + 9, wie="het ronde terras van de Drakendans")
    west, oost = [], []
    for k in range(7):
        a = math.radians(90 + 30 * k)               # from the south, by the west, to the north
        dx, dz = 5 * math.cos(a), 5 * math.sin(a)
        west.append((int(round(tx + dx)), ty + 1, int(round(tz + dz))))
        oost.append((int(round(tx - dx)), ty + 1, int(round(tz + dz))))
    b.pad("drakendans_speler", west, wie="jouw danspassen: de westelijke halve cirkel")
    b.pad("drakendans_zuka", oost, wie="Zuka's danspassen: de oostelijke halve cirkel, in spiegelbeeld")
    b.sign(tx - 2, ty + 1, tz + 8, "north", ["De Drakendans", "Stap voor stap", "Niet struikelen", "(ze kijken)"], wall=False)
    b.geen_licht.update((x, z) for x in range(tx - 6, tx + 7) for z in range(tz - 6, tz + 7))


def drakengrot(b, midden, kleur):
    """A dragon's cave in a peak next to the terrace: a short tunnel and a round chamber that glows in the dragon's colour."""
    tx, tz, ty = TERRAS
    cx, cz = midden
    west = cx < tx
    rand = tx - 9 if west else tx + 9
    licht = LAVA if kleur == "rood" else "sea_lantern"
    steen = [(KOOL, 50), (IJZER, 20), ("red_terracotta", 30)] if kleur == "rood" else [(KOOL, 50), (IJZER, 20), ("light_blue_terracotta", 30)]
    # the chamber
    for x in range(cx - 7, cx + 8):
        for z in range(cz - 7, cz + 8):
            d2 = ((x - cx) / 6.6) ** 2 + ((z - cz) / 6.6) ** 2
            if d2 > 1.0:
                continue
            hoog = max(2, int(round(6.2 * math.sqrt(1 - d2))))
            b.set(x, ty, z, b.kies(steen))
            b.lucht(x, ty + 1, z, x, ty + hoog, z)
            if d2 > 0.6 and (x * 7 + z * 3) % 5 == 0:
                b.set(x, ty, z, licht)
            if (x * 5 + z * 11) % 23 == 0 and hoog >= 4:
                b.set(x, ty + hoog + 1, z, GLOEI if kleur == "rood" else "sea_lantern")
    # the tunnel from the terrace
    for x in range(min(rand, cx), max(rand, cx) + 1):
        if abs(x - cx) < 6:
            continue
        b.fill(x, ty, cz - 1, x, ty, cz + 1, ZAND_GLAD)
        b.lucht(x, ty + 1, cz - 1, x, ty + 3, cz + 1)
        b.lucht(x, ty + 4, cz, x, ty + 4, cz)
        if x % 3 == 0:
            b.set(x, ty, cz - 1, licht)
            b.set(x, ty, cz + 1, licht)
    # the mouth: a carved frame in the dragon's colour
    mx = rand - (1 if west else -1)
    wol = "red_terracotta" if kleur == "rood" else "light_blue_terracotta"
    for z in (cz - 2, cz + 2):
        b.fill(mx, ty + 1, z, mx, ty + 4, z, wol)
    b.fill(mx, ty + 5, cz - 2, mx, ty + 5, cz + 2, wol)
    b.set(mx, ty + 5, cz, ZONNESTEEN)
    # the nest at the back
    nx = cx - 3 if west else cx + 3
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx or dz:
                b.set(nx + dx, ty + 1, cz + dz, GOUD if (dx + dz) % 2 else "raw_gold_block")
    b.set(nx, ty, cz, licht)
    b.geen_licht.add((nx, cz))
    yaw = -90.0 if west else 90.0
    b.plek(f"draak_{kleur}", nx + (3 if west else -3), ty + 1, cz, yaw=yaw, wie=f"de {'rode' if kleur == 'rood' else 'blauwe'} draakguh in zijn grot", kind="DRAAKGUH")
    b.zone(f"drakengrot_{kleur}", cx - 7, ty + 1, cz - 7, cx + 7, ty + 7, cz + 7, wie=f"de grot van de {'rode' if kleur == 'rood' else 'blauwe'} draakguh")
    b.sign(rand + (1 if west else -1), ty + 1, cz + 2, "east" if west else "west",
           ["Draak thuis", "Kloppen helpt", "niet. Dansen", "wel, njeg"] if kleur == "rood" else ["Ook draak thuis", "Niet aaien", "zonder dans", ""], wall=False)


def ruine(b):
    """The ruin at the foot of the stair: a paved court with a sun, broken columns, and the little step pyramid where the
    eternal flame burns (a little lukewarm)."""
    tx = TERRAS[0]
    x0, z0, x1, z1 = 16, 152, 52, 182
    cx, cz = tx, 166
    for x in range(cx - 5, cx + 6):
        for z in range(cz - 5, cz + 6):
            d = math.hypot(x - cx, z - cz)
            hoek = math.degrees(math.atan2(z - cz, x - cx)) % 45
            if d < 1.5:
                b.set(x, G, z, GOUD)
            elif d < 5.4 and (abs(d - 4.5) < 0.6 or min(hoek, 45 - hoek) < 6):
                b.set(x, G, z, ZONNESTEEN)
    # broken columns along the court (some whole, some stumps, one fallen)
    for k, (x, z) in enumerate([(x0 + 3, z0 + 3), (x0 + 9, z0 + 3), (x1 - 9, z0 + 3), (x1 - 3, z0 + 3), (x0 + 3, z1 - 3), (x1 - 3, z1 - 3),
                                (x1 - 3, cz), (x1 - 9, z1 - 3)]):
        hoog = (5, 2, 4, 1, 3, 5, 2, 4)[k]
        b.set(x, G, z, ZAND_GLAD)
        b.fill(x, V, z, x, V + hoog - 1, z, "sandstone_wall" if hoog < 3 else "cut_sandstone")
        if hoog >= 4:
            b.set(x, V + hoog, z, ZAND_BEITEL)
            b.set(x, V + hoog + 1, z, GLOEI)
            b.vlam(x, V + hoog + 2, z)
        b.geen_licht.add((x, z))
    for x in range(x1 - 8, x1 - 4):
        b.set(x, V, cz + 6, "cut_sandstone")
    # the step pyramid with the eternal flame
    px, pz = x0 + 8, cz
    for k in range(3):
        b.fill(px - 4 + k, V + k, pz - 4 + k, px + 4 - k, V + k, pz + 4 - k, ZAND if k else ZAND_GLAD)
        for z in range(pz - 1, pz + 2):
            b.trap(px + 5 - k - 1 + 1 - 1, V + k, z, ZAND_TRAP, "west")
    b.fill(px - 1, V + 3, pz - 1, px + 1, V + 3, pz + 1, ZONNESTEEN)
    b.set(px, V + 3, pz, LAVA)
    b.vlam(px, V + 4, pz)
    for dx in (-1, 1):
        for dz in (-1, 1):
            b.fill(px + dx, V + 4, pz + dz, px + dx, V + 5, pz + dz, ZAND_MUUR)
            b.set(px + dx, V + 6, pz + dz, ZAND_BEITEL)
    b.fill(px - 1, V + 7, pz - 1, px + 1, V + 7, pz + 1, GOUD)
    b.prop("eeuwige_vlam", px, V + 4, pz, wie="de eeuwige vlam boven op de trappiramide")
    b.sign(px + 5, V, pz + 2, "east", ["Eeuwige vlam", "Brandt al", "duizend jaar", "(beetje lauw)"], wall=False)
    b.plek("zonnekrijger", cx + 3, V, cz, yaw=90.0, wie="het hoofd van de Zonnekrijgers", kind="ZONNEKRIJGER_GUH")
    for k, (x, z) in enumerate(((cx - 3, z0 + 6), (cx + 6, z1 - 6)), 1):
        b.plek(f"zonnekrijger_wacht_{k}", x, V, z, yaw=0.0 if k == 1 else 180.0, wie="een Zonnekrijger-guh", kind="ZONNEKRIJGER_GUH")
    b.sign(tx + 5, V, z0 + 1, "south", ["Naar de draken", "34 treden", "Wie zeurt:", "68 treden"], wall=False)
    # the gate at the foot of the stair: two pillars and what is left of the lintel
    for x in (tx - 4, tx + 4):
        b.set(x, G, z0, ZAND_GLAD)
        b.fill(x, V, z0, x, V + 5, z0, ZAND_GLAD)
        b.set(x, V + 6, z0, ZAND_BEITEL)
    for x in range(tx - 4, tx + 2):
        b.set(x, V + 7, z0, ZONNESTEEN if x == tx - 1 else ZAND)
    b.trap(tx + 2, V + 7, z0, ZAND_TRAP, "west", "top")
    # what is left of the wall around the court (higher at the corners, gone in places), rubble in the corners
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if not (x in (x0, x1) or z in (z0, z1)) or (abs(x - tx) < 5 and z == z0):
                continue
            if (x == x1 and 164 <= z <= 170) or (z == z1 and 42 <= x <= 50):
                continue                                   # (the ways in: from the east and from the beach)
            hoek = min(abs(x - x0), abs(x - x1)) + min(abs(z - z0), abs(z - z1))
            hoog = max(0, 3 - hoek // 3 + b.rng.choice((-1, 0, 0, 1)))
            if (x * 7 + z * 13) % 11 < 3:
                hoog = 0
            for k in range(min(hoog, 4)):
                b.set(x, V + k, z, b.kies([(ZAND, 50), (ZAND_GLAD, 30), ("smooth_sandstone", 20)]))
    for x, z in ((x0 + 2, z0 + 1), (x0 + 1, z1 - 2), (x1 - 2, z1 - 1), (x1 - 1, z0 + 2), (x0 + 14, z1 - 2), (x1 - 12, z0 + 6)):
        b.set(x, V, z, b.kies([(ZAND, 60), (ZAND_BEITEL, 20), ("sandstone_slab", 20)]))
    b.prop("rustpunt_7", tx, V, z0 + 4, wie="rustpunt: de ruïne, voor de lange trap")


# =====================================================================================================================
# the duel arena
# =====================================================================================================================
def arena(b):
    """The rock pillars in the crater lake. The first one is reached over a causeway from the south shore; from there you
    jump (or fly). Markers: every pillar top, the two duel spots, the part of the arena each round uses."""
    g = b.grond
    for k, (dx, dz, r, y) in enumerate(PILAREN, 1):
        x, z = A[0] + dx, A[1] + dz
        b.set(x, y, z, GLOEI if k % 2 else GLAD)
        b.geen_licht.add((x, z))
        b.prop(f"pilaar_{k:02d}", x, y, z, wie=f"de top van rotspilaar {k}", los=k > 1)
    sx, sz, _, sy = PILAREN[2]
    mx, mz, _, my = PILAREN[7]
    b.prop("duel_speler", A[0] + sx + 1, sy + 1, A[1] + sz, wie="waar jij staat bij het begin van het duel", los=True)
    b.prop("duel_mikai", A[0] + mx - 1, my + 1, A[1] + mz, wie="waar Vuurheer Mikai staat", los=True)
    b.zone("duel_arena", A[0] - AR - 2, A_WATER - 4, A[1] - AR - 2, A[0] + AR + 2, 84, A[1] + AR + 2, wie="de duelarena: rotspilaren boven het water")
    b.zone("ronde_lucht", A[0] - 12, A_WATER, A[1] - 21, A[0] + 12, 84, A[1] - 9, wie="ronde 1 (lucht): de hoge pilaren in het noorden")
    b.zone("ronde_water", A[0] - 13, A_WATER - 4, A[1] + 4, A[0] + 14, 60, A[1] + 20, wie="ronde 2 (water): de lage pilaren boven het water in het zuiden")
    b.zone("ronde_aarde", A[0] - 19, A_WATER, A[1] - 10, A[0] - 3, 70, A[1] + 6, wie="ronde 3 (aarde): de pilaren in het westen")
    b.zone("ronde_vuur", A[0] - 5, A_WATER, A[1] - 9, A[0] + 19, 76, A[1] + 7, wie="ronde 4 (vuur): de brede middenpilaar en het oosten")
    b.prop("komeet_zicht", A[0], 114, A[1] - 24, wie="waar de knabbelkomeet aan de hemel staat, gezien vanaf de arena", los=True)
    # the dent of the shard on the west shore
    kx, kz = 23, 50
    y = g.hoogte(kx, kz)
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            d = math.hypot(dx, dz)
            if 1.2 < d < 2.6:
                b.set(kx + dx, y, kz + dz, GLOEI if (dx + dz) % 2 else KOOL)
    b.set(kx, y, kz, "guhs:gepolijst_roosterijzer")
    b.prop("komeetscherf", kx, y + 1, kz, wie="waar na het duel de scherf van de knabbelkomeet ligt")
    b.geen_licht.update((kx + dx, kz + dz) for dx in range(-2, 3) for dz in range(-2, 3))
    b.sign(A[0] - 2, V, A[1] + 31, "south", ["Duelplaats", "Zwemvest", "aanbevolen", "njeg"], wall=False)
    b.prop("rustpunt_8", A[0] - 4, V, A[1] + 30, wie="rustpunt: de oever van de arena")


# =====================================================================================================================
# Sintelstrand
# =====================================================================================================================
def sintelstrand(b):
    g = b.grond
    # --- the beach house: bamboo walls, a veranda on the sea side, dust inside ---
    x0, z0, x1, z1 = 66, 192, 82, 203
    ix0, iz0, ix1, iz1, deur, top = huis(b, x0, z0, x1, z1, G, "south", wand="bamboo_planks",
                                        bord=["Strandhuis van", "de Vuurheer", "Al jaren leeg", "(wel gezellig)"])
    v = V
    for x in range(x0, x1 + 1):
        for z in range(z1 + 1, z1 + 4):
            b.set(x, G, z, "bamboo_mosaic")
        if x % 4 == 2:
            b.fill(x, V, z1 + 3, x, V + 2, z1 + 3, "bamboo_fence")
    b.slaapzak(ix0, v, iz0, "west")
    b.slaapzak(ix0, v, iz0 + 2, "west")
    b.set(ix1, v, iz0, "cobweb")
    b.set(ix1, v + 3, iz1, "cobweb")
    b.set(ix0 + 6, v, iz0, PORTRET)
    b.sign(ix0 + 5, v + 1, iz0, "south", ["Familieportret", "Iedereen lacht", "(het is een", "oud portret)"])
    b.set(ix0 + 8, v, iz0, "guhs:guh_kast", {"facing": "south", "open": "false"})
    b.set(ix1 - 2, v, iz0 + 3, "guhs:guh_tafel", {"facing": "north"})
    b.set(ix1 - 3, v, iz0 + 3, "guhs:guh_stoel", {"facing": "east"})
    b.set(ix1 - 1, v, iz0 + 3, "guhs:guh_stoel", {"facing": "west"})
    b.kist(ix1, v, iz1, "west", "strandhuis")
    b.tapijt(ix0 + 3, v, iz0 + 4, ix0 + 6, iz0 + 6, "orange")
    for x in (x0 + 3, x1 - 3):
        b.set(x, V, z1 + 2, "guhs:red_zitzak")
    b.zone("strandhuis", x0, V, z0, x1, V + 9, z1 + 3, wie="het oude strandhuis van de familie van de Vuurheer")
    # --- the open-air theatre: a stage with a painted backdrop, five tiers of seats ---
    tx0, tx1, tz = 36, 60, 221
    b.fill(tx0 + 4, V, tz, tx1 - 4, V, tz + 6, HOUT)
    b.fill(tx0 + 4, G, tz, tx1 - 4, G, tz + 6, STENEN)
    for x in range(tx0 + 4, tx1 - 3):
        b.trap(x, V, tz - 1, HOUT_TRAP, "south")
    # the backdrop: a night sky with the kaasbol the boy was found in, curtains at the sides
    for x in range(tx0 + 4, tx1 - 3):
        for y in range(V + 1, V + 8):
            b.set(x, y, tz + 6, "blue_wool" if y > V + 2 else "white_wool")
    bx, by = (tx0 + tx1) // 2, V + 4
    for dx in range(-2, 3):
        for dy in range(-2, 3):
            if dx * dx + dy * dy <= 5:
                b.set(bx + dx, by + dy, tz + 6, "yellow_concrete" if (dx + dy) % 3 else "orange_concrete")
    for x in (tx0 + 4, tx0 + 5, tx1 - 5, tx1 - 4):
        b.fill(x, V + 1, tz + 5, x, V + 7, tz + 5, "red_wool")
    b.fill(tx0 + 4, V + 8, tz + 5, tx1 - 4, V + 8, tz + 6, "red_wool")
    for x in (tx0 + 6, bx, tx1 - 6):
        b.set(x, V, tz + 1, GLOEI)
    for x in (tx0 + 3, tx1 - 3):
        b.vuurpot(x, V, tz - 1, 2)
    b.zone("toneel", tx0 + 4, V + 1, tz, tx1 - 4, V + 7, tz + 5, wie="het toneel")
    for k, dx in enumerate((-6, -3, 0, 3, 6), 1):
        b.plek(f"acteur_{k}", bx + dx, V + 1, tz + 3, yaw=180.0, wie=["de acteur die Aang-guh speelt (een meisje)", "de acteur die Katara-guh speelt",
               "de acteur die Sokka-guh speelt", "de acteur die Toph-guh speelt (een reus)", "de acteur die Zuka speelt (litteken verkeerd)"][k - 1],
               kind="TONEELSPELER_GUH")
    # the tiers: five rows of seats that rise to the north, away from the stage, each row a step of stone two deep (you
    # walk in front of the seats); an aisle of stairs in the middle
    for k in range(5):
        z = tz - 5 - 2 * k
        for x in range(tx0 + 2 - k, tx1 - 1 + k):
            if k:
                b.fill(x, V, z, x, V + k - 1, z + 1, STENEN)
            if abs(x - bx) <= 1:
                b.trap(x, V + k, z, STENEN_TRAP, "north")
            else:
                b.stoel(x, V + k, z, "south")
        if k % 2 == 0:
            for x in (tx0 + 1 - k, tx1 - 1 + k):
                b.vuurpot(x, V, z + 1, 1 + k)
    b.prop("publiek_speler", bx - 3, V + 2, tz - 9, wie="jouw stoel op de tribune (derde rij)")
    b.sign(bx - 9, V, tz - 2, "north", ["Vanavond:", "De jongen in", "de kaasbol", "(vrij naar)"], wall=False)
    b.sign(bx + 9, V, tz - 2, "north", ["Effecten:", "geweldig", "De rest:", "njeg..."], wall=False)
    b.zone("theater", tx0 - 4, V, tz - 16, tx1 + 4, V + 10, tz + 7, wie="het openluchttheater van Sintelstrand")
    # --- beach things: parasols, towels, a sand castle with the emblem, embers ---
    for x, z, kleur in ((30, 206, "red"), (74, 214, "orange"), (88, 222, "red"), (24, 216, "yellow")):
        if g.land[x, z] and (x, z) not in g.nat:
            y = g.hoogte(x, z)
            b.fill(x, y + 1, z, x, y + 3, z, HOUT_HEK)
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    b.set(x + dx, y + 4, z + dz, f"{kleur}_wool" if (dx + dz) % 2 == 0 else "white_wool")
            b.set(x + 1, y + 1, z + 1, f"{kleur}_carpet")
            b.geen_licht.add((x, z))
    sx, sz = 80, 220
    if (sx, sz) not in g.nat:
        y = g.hoogte(sx, sz)
        b.fill(sx - 1, y + 1, sz - 1, sx + 1, y + 1, sz + 1, "sandstone")
        b.set(sx, y + 2, sz, "sandstone_wall")
        b.set(sx, y + 3, sz, EMBLEEM)
        b.sign(sx, y + 1, sz + 2, "south", ["Zandkasteel", "van prinses", "Azula. Blijf af", "(ze ziet alles)"], wall=False)
    b.sign(64, V, 208, "south", ["Sintelstrand", "Zwemmen: lauw", "Zonnen: zonder", "zon, njeg"], wall=False)
    nat = sorted(c for c in g.nat if 40 <= c[0] <= 70 and g.nat[c][0] == HAVEN_WATER)
    for k, c in enumerate((nat[len(nat) // 3], nat[2 * len(nat) // 3]), 5):
        b.wezen(f"eendguh_{k}", c[0], HAVEN_WATER + 1, c[1], "guhs:schildpad_eendguh", yaw=45.0 * k, honk=7)
    b.zone("sintelstrand", 20, G - 5, 186, 96, V + 12, 250, wie="Sintelstrand")
    b.prop("rustpunt_9", 64, V, 210, wie="rustpunt: Sintelstrand")


# =====================================================================================================================
# the camp of the Witte Lotus
# =====================================================================================================================
def tent(b, x0, z0, x1, z1, y, kant, streep="light_blue_wool"):
    """A tent on the rectangle: white walls two high, a stepped roof with stripes, an opening of three on side `kant`."""
    b.fill(x0, y, z0, x1, y, z1, "white_wool")
    b.fill(x0 + 1, y, z0 + 1, x1 - 1, y, z1 - 1, "birch_planks")
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                b.set(x, y + 1, z, "white_wool")
                b.set(x, y + 2, z, streep if (x + z) % 2 else "white_wool")
            else:
                b.lucht(x, y + 1, z, x, y + 2, z)
    # the roof: layers that step in, each two cells wide so that it rests on the one below
    k = 0
    while 2 * k <= min(x1 - x0, z1 - z0):
        for x in range(x0 + k, x1 - k + 1):
            for z in range(z0 + k, z1 - k + 1):
                if min(x - x0, x1 - x, z - z0, z1 - z) <= k + 1:
                    b.set(x, y + 3 + k, z, "white_wool" if k % 2 == 0 else streep)
        k += 1
    xm, zm = (x0 + x1) // 2, (z0 + z1) // 2
    dx, dz = gb.DIRS[kant]
    deur = {"north": (xm, z0), "south": (xm, z1), "west": (x0, zm), "east": (x1, zm)}[kant]
    for j in (-1, 0, 1):
        b.lucht(deur[0] + (j if dz else 0), y + 1, deur[1] + (j if dx else 0), deur[0] + (j if dz else 0), y + 2, deur[1] + (j if dx else 0))
    b.hanglamp(xm, y + 2 + k, zm, 1)
    b.geen_licht.add((xm, zm))
    return x0 + 1, z0 + 1, x1 - 1, z1 - 1, deur


def lotus_kamp(b):
    """The tents of the Orde van de Witte Lotus on a patch of unburnt crust: the big tent of Oom Miroh (tea, a lotus in the
    floor), three small ones, a fire, a practice corner."""
    y = G
    v = y + 1
    # --- the big tent ---
    ix0, iz0, ix1, iz1, deur = tent(b, 222, 98, 236, 110, y, "west")
    cx, cz = 229, 104
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            b.set(cx + dx, y, cz + dz, LOTUS)
    b.tapijt(ix0, v, iz0, ix1, iz0 + 1, "light_blue")
    b.tapijt(ix0, v, iz1 - 1, ix1, iz1, "light_blue")
    b.set(cx + 4, v, cz, "guhs:theetafel", {"gedekt": "true"})
    b.set(cx + 4, v, cz - 2, "guhs:theepotje", {"facing": "west"})
    b.set(cx + 5, v, cz + 2, "guhs:guh_slaapzak", {"facing": "west", "occupied": "false"})
    for dx, dz, kleur in ((3, -1, "white"), (3, 1, "light_blue"), (5, 0, "white")):
        b.set(cx + dx, v, cz + dz, f"guhs:{kleur}_kussen")
    b.kist(ix1, v, iz0, "west", "lotus")
    for x, z in ((ix0, iz0), (ix0, iz1), (ix1, iz1)):
        b.lantaarn(x, v, z, False)
    b.plek("miroh_tent", cx + 2, v, cz, yaw=90.0, wie="Oom Miroh in zijn tent", kind="OOM_MIROH")
    b.prop("verzoening_zuka", cx - 2, v, cz, wie="waar Zuka knielt: 'Ik was nooit boos op je'")
    b.zone("lotus_tent", 222, v, 98, 236, v + 8, 110, wie="de tent van Oom Miroh")
    b.sign(221, v + 1, cz + 2, "west", ["Oom Miroh", "Thee staat klaar", "Zorgen mogen", "buiten blijven"])
    # --- three small tents, each with its master in front ---
    for (x0, z0, kant, kleur, id, wie, kind, yaw, px, pz) in (
            (212, 90, "south", "green_wool", "bumi", "Bumi-guh, die net een steen opat", "BUMI_GUH", 0.0, 215, 97),
            (212, 112, "north", "blue_wool", "pakguh_lotus", "Meester Pakguh met zijn snor", "PAKGUH", 180.0, 215, 110),
            (238, 114, "north", "gray_wool", "piandao", "Piandao-guh, de zwaardmeester", "PIANDAO_GUH", 180.0, 241, 112)):
        jx0, jz0, jx1, jz1, d = tent(b, x0, z0, x0 + 6, z0 + 6, y, kant, streep=kleur)
        b.set(jx0, v, jz0, "guhs:guh_slaapzak", {"facing": "east", "occupied": "false"})
        b.ton(jx1, v, jz0)
        b.plek(id, px, v, pz, yaw=yaw, wie=wie, kind=kind)
    # --- the fire in the middle, the lotus on a pole, the practice corner ---
    fx, fz = 217, 104
    b.set(fx, y, fz, GLOEI)
    b.vlam(fx, v, fz)
    for dx, dz, kant in ((-2, 0, "east"), (0, -2, "south"), (0, 2, "north")):
        b.stoel(fx + dx, v, fz + dz, kant, "birch_stairs")
    b.fill(219, v, 100, 219, v + 4, 100, "birch_fence")
    b.set(219, v + 5, 100, LOTUS)
    b.set(219, y, 100, "birch_planks")
    for x, z in ((240, 100), (243, 104)):
        b.set(x, y, z, "birch_planks")
        b.set(x, v, z, "birch_fence")
        b.set(x, v + 1, z, "hay_block", {"axis": "y"})
        b.set(x, v + 2, z, "carved_pumpkin", {"facing": "west"})
        b.geen_licht.add((x, z))
    b.sign(241, v, 102, "west", ["Oefenpoppen", "Ze duwen niet", "terug. Meestal", "njeg"], wall=False)
    b.sign(220, v, 101, "west", ["Orde v.d. Witte", "Lotus. Oude", "meesters,", "verse thee"], wall=False)
    b.geen_licht.update(((fx, fz), (219, 100)))
    b.zone("lotus_kamp", 208, v, 84, 248, v + 9, 124, wie="het tentenkamp van de Witte Lotus")
    b.prop("rustpunt_10", 218, v, 107, wie="rustpunt: het kamp van de Witte Lotus")


# =====================================================================================================================
# the quay and the Leeuwschildpad-guh
# =====================================================================================================================
def kade(b):
    """The quay at the bite in the rim: two stone lanterns and a sign. The Leeuwschildpad-guh surfaces right in front."""
    kx, kz = KADE
    for x in (kx - 4, kx + 4):
        b.set(x, G, kz + 2, ZWART)
        b.vuurpot(x, V, kz + 2, 1, paal=STENEN_MUUR)
        b.geen_licht.add((x, kz + 2))
    b.sign(kx + 3, V, kz + 4, "north", ["Niet voeren:", "eilanden", "(soms is het", "er eentje)"], wall=False)
    b.prop("leeuwschildpad_kade", kx, V, kz - 2, wie="waar je staat als je met de Leeuwschildpad-guh praat (zijn kin ligt op de kade)")
    b.zone("kade", kx - 4, V, kz - 3, kx + 4, V + 6, kz + 4, wie="de kade bij de hap uit de rand")
    b.prop("rustpunt_11", kx, V, kz + 3, wie="rustpunt: de kade")


def leeuwschildpad(b):
    """The overlay laag_leeuwschildpad (45 x 33 x 57, blocks only): the Leeuwschildpad-guh surfacing in the bite of the rim,
    head to the south with its chin against the quay. A shell with a little wood on it, a guh's face with a mane, four
    flippers. Returns the Structure; its markers are recorded in the build's coordinates."""
    s = b.h.Structure((45, 33, 57))
    ox, oy, oz = LAAG
    cx, cz, wy = 22, 26, 14                 # the middle of the shell and the "water line" (the world's ground level)
    assert oy + wy == G, (oy, wy)
    schild = [("green_terracotta", 44), ("moss_block", 26), ("lime_terracotta", 16), ("guhs:kaasmos", 14)]
    buik = [("yellow_terracotta", 60), ("guhs:kaaskorst", 40)]
    top = {}
    for x in range(45):
        for z in range(57):
            d2 = ((x - cx) / 17.0) ** 2 + ((z - cz) / 21.0) ** 2
            if d2 > 1.0:
                continue
            koepel = int(round(9.0 * math.sqrt(1 - d2) ** 1.3))
            onder = int(round(7.0 * math.sqrt(1 - d2)))
            for y in range(wy - onder, wy + koepel + 1):
                rand = y >= wy + koepel - 1 or y <= wy - onder + 1 or d2 > 0.86
                if not rand:
                    continue
                if y < wy:
                    s.set(x, y, z, gb.mc(b.kies(buik)))
                else:
                    # the plates of the shell: lines of yellow between green fields
                    naad = (x - cx) % 7 == 0 or (z - cz + (3 if ((x - cx) // 7) % 2 else 0)) % 8 == 0
                    s.set(x, y, z, "minecraft:yellow_terracotta" if naad and y >= wy + koepel - 1 else gb.mc(b.kies(schild)))
            top[(x, z)] = wy + koepel
    # the little wood on the back: three blossom trees and a pond of light
    for tx, tz in ((cx - 6, cz - 6), (cx + 5, cz + 2), (cx - 3, cz + 9)):
        ty = top[(tx, tz)]
        for y in range(ty + 1, ty + 5):
            s.set(tx, y, tz, "guhs:guhbloesem_log", {"axis": "y"})
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                for dy in range(3, 7):
                    if abs(dx) + abs(dz) + abs(dy - 4) <= 3 and not (dx == 0 and dz == 0 and dy < 5):
                        s.set(tx + dx, ty + dy, tz + dz, "guhs:guhbloesem_leaves", dict(BLAD))
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            s.set(cx + dx, top[(cx, cz)], cz + dz, gb.mc(GLOEI) if (dx or dz) else "minecraft:sea_lantern")
    # the head: a big guh's face (pink, round ears, black eyes) with a lion's mane, the chin stretched out to the quay
    hz0, hz1 = 44, 56
    for x in range(cx - 6, cx + 7):
        for z in range(hz0, hz1 + 1):
            for y in range(wy - 4, wy + 9):
                d2 = ((x - cx) / 6.4) ** 2 + ((z - 50) / 6.6) ** 2 + ((y - (wy + 2)) / 6.2) ** 2
                if d2 <= 1.0:
                    s.set(x, y, z, "minecraft:pink_concrete" if d2 > 0.5 else "minecraft:pink_terracotta")
    for x in range(cx - 8, cx + 9):
        for y in range(wy - 3, wy + 11):
            for z in (hz0 - 1, hz0, hz0 + 1):
                d = math.hypot(x - cx, y - (wy + 3))
                if 6.0 <= d <= 8.4 and y >= wy - 2:
                    s.set(x, y, z, "minecraft:orange_wool" if (x + y) % 3 else "minecraft:yellow_wool")
    for ex in (cx - 5, cx + 5):
        for dx in (-1, 0, 1):
            for dy in (0, 1, 2):
                s.set(ex + dx, wy + 8 + dy, 48, "minecraft:pink_concrete" if (dx, dy) != (0, 1) else "minecraft:magenta_concrete")
    for ex in (cx - 3, cx + 3):
        for dy in (0, 1):
            s.set(ex, wy + 3 + dy, hz1, "minecraft:black_concrete")
    s.set(cx, wy + 2, hz1, "minecraft:magenta_concrete")
    for dx in (-2, -1, 0, 1, 2):
        s.set(cx + dx, wy, hz1, "minecraft:black_concrete" if abs(dx) == 2 else "minecraft:pink_terracotta")
        s.set(cx + dx, wy - 1, hz1, "minecraft:black_concrete" if abs(dx) < 2 else "minecraft:pink_concrete")
    # the neck between the shell and the head
    for x in range(cx - 3, cx + 4):
        for z in range(cz + 19, hz0 + 2):
            for y in range(wy - 3, wy + 4):
                if ((x - cx) / 3.4) ** 2 + ((y - wy) / 3.6) ** 2 <= 1.0:
                    s.set(x, y, z, "minecraft:pink_terracotta")
    # four flippers
    for fx, fz, lang in ((cx - 15, cz + 12, -1), (cx + 15, cz + 12, 1), (cx - 14, cz - 14, -1), (cx + 14, cz - 14, 1)):
        for k in range(7):
            for dz in range(-2 - (k < 4), 3 + (k < 4)):
                x = fx + lang * k
                if 0 <= x < 45:
                    s.set(x, wy - 1, fz + dz, "minecraft:pink_terracotta")
                    s.set(x, wy - 2, fz + dz, "minecraft:pink_concrete")
    # flat spots: where you wake up on its back, and on its snout (the talk spot)
    rug = (cx + 2, top[(cx + 2, cz - 3)], cz - 3)
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            s.set(rug[0] + dx, rug[1], rug[2] + dz, "minecraft:moss_block")
            for dy in (1, 2, 3):
                s.blocks.pop((rug[0] + dx, rug[1] + dy, rug[2] + dz), None)
    snuit_y = max(y for (x, y, z) in s.blocks if x == cx and z == hz1 - 2)
    b.prop("leeuwschildpad_anker", ox, oy, oz, wie="de oorsprong van de laag laag_leeuwschildpad (in de hap uit de rand)", los=True)
    b.prop("leeuwschildpad_rug", ox + rug[0], oy + rug[1] + 1, oz + rug[2], wie="waar je wakker wordt op zijn rug", los=True)
    b.plek("leeuwschildpad", ox + cx, oy + snuit_y + 1, oz + hz1 - 2, yaw=0.0, wie="de Leeuwschildpad-guh: boven op zijn snuit, bij zijn gezicht",
           kind="LEEUWSCHILDPAD_GUH", los=True)
    return s


# =====================================================================================================================
# the nature of a volcano
# =====================================================================================================================
def natuur(b):
    """Smoke holes on the crests, embers on the scorched ground, streams of lukewarm lava down the walls of the big crater."""
    g = b.grond
    rng = b.rng
    vrij = lambda x, z: g.land[x, z] and not g.vast[x, z] and (x, z) not in g.nat and b.naam(x, g.hoogte(x, z) + 1, z) is None  # noqa: E731
    # streams: from the crest down the inner wall, following the steepest way
    for hoek in (20, 75, 160, 215, 290, 340):
        a = math.radians(hoek)
        x, z = int(round(C[0] + 60 * math.cos(a))), int(round(C[1] + 60 * math.sin(a)))
        for _ in range(40):
            if not (g.land[x, z] and not g.vast[x, z]) or g.hoogte(x, z) <= F:
                break
            y = g.hoogte(x, z)
            if b.naam(x, y + 1, z) is None:
                b.set(x, y, z, LAVA)
            buren = [(g.hoogte(x + dx, z + dz), rng.random(), x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))
                     if g.land[x + dx, z + dz] and math.hypot(x + dx - C[0], z + dz - C[1]) < math.hypot(x - C[0], z - C[1]) + 0.3]
            if not buren:
                break
            buren.sort()
            _, _, x, z = buren[1] if len(buren) > 1 and rng.random() < 0.3 else buren[0]
    # smoke holes on the crests of both craters, embers and little ash heaps on the open ground
    for _ in range(2600):
        x, z = rng.randrange(4, 252), rng.randrange(4, 252)
        if not vrij(x, z):
            continue
        y = g.hoogte(x, z)
        soort = int(g.soort[x, z])
        onder = b.naam(x, y, z)
        if soort == gr.GLOED and rng.random() < 0.10:
            b.set(x, y + 1, z, "guhs:rookgat")
        elif soort in (gr.ROTS, gr.ZAND) and onder in (KOOL, "guhs:as_aarde") and rng.random() < 0.30:
            b.set(x, y + 1, z, "guhs:smeulkooltjes")
        elif soort == gr.KORST and onder == "guhs:kaaskorst" and rng.random() < 0.5:
            b.set(x, y + 1, z, "guhs:korstgras")
    # signposts along the ways around the crater
    for x, z, kant, regels in ((128, 12, "south", ["Noordpad", "Links: afgrond", "Rechts: vulkaan", "Kies wijs, njeg"]),
                               (208, 62, "south", ["Naar de kade", "Niet zwaaien", "naar eilanden", "(ze zwaaien ook)"]),
                               (170, 161, "south", ["Tussen 2 kraters", "Links: de stad", "Rechts: de bak", "Rechtdoor: thee"]),
                               (84, 17, "south", ["Naar de arena", "Zwemdiploma", "en moed", "meenemen"])):
        y = g.hoogte(x, z)
        if b.naam(x, y + 1, z) is None:
            b.sign(x, y + 1, z, kant, regels, wall=False)
            b.geen_licht.add((x, z))
    # steam around the boiling lake: smoke holes on its shore
    for hoek in range(0, 360, 18):
        a = math.radians(hoek + 5)
        x, z = int(round(K[0] + (KRV + 1) * math.cos(a))), int(round(K[1] + (KRV + 1) * math.sin(a)))
        if vrij(x, z) and g.hoogte(x, z) <= MEER + 3:
            b.set(x, g.hoogte(x, z) + 1, z, "guhs:rookgat")


# =====================================================================================================================
# what the kit's check does not know
# =====================================================================================================================
SCHADELIJK = ("minecraft:lava", "minecraft:fire", "minecraft:soul_fire", "minecraft:magma_block", "minecraft:campfire", "minecraft:soul_campfire",
              "minecraft:cactus", "minecraft:sweet_berry_bush", "minecraft:wither_rose", "minecraft:powder_snow", "minecraft:pointed_dripstone",
              "minecraft:tnt", "minecraft:respawn_anchor")
# blocks of the mod that do something a public place should not offer: the work stations a tamed guh takes a job at, the
# throne of the Koningguh, things you eat away
NIET_HIER = ("guhs:knabbelbak", "guhs:naaitafel", "guhs:mikatrofee", "guhs:opper_mikatrofee", "guhs:koningstroon", "guhs:knabbelkorf",
             "guhs:guh_taart", "guhs:guh_voerbak")


# properties a blockstate file does not have to mention (they change nothing about the model)
STIL = {"waterlogged", "powered", "occupied", "distance", "persistent", "level", "has_record", "note", "instrument", "triggered", "lit",
        "has_book", "open", "candles", "in_wall", "hinge", "part", "facing", "fase", "voer", "bites", "gedekt", "axis", "type", "half", "shape",
        "face", "hanging", "rotation", "north", "south", "east", "west", "up"}


def _toestanden(h, name):
    """property -> the values the block's blockstate file knows (None: the block does not exist)."""
    import json
    import os
    import zipfile
    ns, id = name.split(":")
    if ns == "guhs":
        path = f"{h.A}/blockstates/{id}.json"
        if not os.path.exists(path):
            return None
        data = json.load(open(path, encoding="utf-8"))
    else:
        if not hasattr(h, "_bouw_vuur_jar"):
            h._bouw_vuur_jar = zipfile.ZipFile(os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar"))
        try:
            data = json.loads(h._bouw_vuur_jar.read(f"assets/minecraft/blockstates/{id}.json"))
        except KeyError:
            return None
    out = {}

    def lees(when):
        for k, v in when.items():
            if k in ("OR", "AND"):
                for deel in v:
                    lees(deel)
            else:
                out.setdefault(k, set()).update(str(v).split("|"))
    for key in data.get("variants", {}):
        for deel in key.split(","):
            if "=" in deel:
                k, v = deel.split("=")
                out.setdefault(k, set()).add(v)
    for deel in data.get("multipart", []):
        lees(deel.get("when", {}))
    # (a multipart file only names the values that draw something: the arms of a fence, the sides of a wall)
    for waarden in out.values():
        if waarden & {"true", "false"}:
            waarden |= {"true", "false"}
        if waarden & {"low", "tall"}:
            waarden |= {"none", "low", "tall"}
    return out


def blok_fouten(b, blokken):
    """Every block of the build exists (in the mod or in vanilla 1.21.1, the format the templates are written in) and every
    state it is written with is one its blockstate file knows: a typing error would silently become another block."""
    p = []
    gezien = {}
    for name, props, _ in blokken:
        sleutel = (name, tuple(sorted(props.items())))
        if sleutel in gezien or name == gb.AIR:
            continue
        gezien[sleutel] = True
        weet = _toestanden(b.h, name)
        if weet is None:
            p.append(f"blok {name} bestaat niet")
            continue
        for k, v in props.items():
            if k in weet:
                if str(v) not in weet[k]:
                    p.append(f"blok {name}: {k}={v} bestaat niet (wel: {sorted(weet[k])})")
            elif k not in STIL:
                p.append(f"blok {name}: de eigenschap {k} staat niet in zijn blockstate")
    return p


def eigen_controle(b):
    p = []
    blocks = b.s.blocks
    laag_ = getattr(b, "laag_structuur", None)
    p += blok_fouten(b, list(blocks.values()) + (list(laag_.blocks.values()) if laag_ is not None else []))
    # (a bed explodes in the Guheinde when you click it: the dimension's bed rule)
    fout = sorted({bl[0] for bl in blocks.values() if bl[0] in SCHADELIJK or bl[0].endswith("_bed")})
    if fout:
        p.append(f"blokken die pijn doen: {fout}")
    fout = sorted({bl[0] for bl in blocks.values() if bl[0] in NIET_HIER})
    if fout:
        p.append(f"blokken die hier niet horen (werkplekken, eetbaar): {fout}")
    # every basin closed, block by block: next to and under a cell of water or sauce is never nothing
    lek = []
    for (x, y, z), bl in blocks.items():
        if bl[0] in ("minecraft:water", SAUS):
            for n in ((x + 1, y, z), (x - 1, y, z), (x, y, z + 1), (x, y, z - 1), (x, y - 1, z)):
                nb = blocks.get(n)
                if nb is None or nb[0] == gb.AIR or b.is_dun(nb[0]):
                    lek.append(((x, y, z), n))
    if lek:
        p.append(f"{len(lek)} plekken waar water of saus weg kan, bv. {lek[:4]}")
    if len(b.s.entities) > 60:
        p.append(f"{len(b.s.entities)} entities (hooguit 60)")
    laag = getattr(b, "laag_structuur", None)
    if laag is not None:
        if any(bl[2] is not None for bl in laag.blocks.values()):
            p.append("de laag heeft blokken met een block entity")
        if any(bl[0] in SCHADELIJK or bl[0] in NIET_HIER or bl[0] in ("minecraft:water", SAUS) for bl in laag.blocks.values()):
            p.append("de laag heeft vloeistof of blokken die pijn doen")
    return p
