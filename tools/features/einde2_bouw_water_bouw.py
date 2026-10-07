"""
einde2 (bouw_water): the Noordelijke Waterstam put together. bouw(h) makes the whole build:

  1. the ground (einde2_bouw_water_grond.Grond): the floating berg with glacier, tiers, wall, canals, sea;
  2. what stands on it (einde2_bouw_water_stad) and Zhao's flagship (einde2_bouw_water_schip);
  3. the markers of the build contract (6.7): where the characters, props, routes and scenes of the story come;
  4. light: the Guheinde has no sky light, so after the lanterns of the builders verlicht() lays ice lanterns in every
     floor that is still dark (sea lanterns in the beds of the water, froglights in the oasis, glowing coals on the ship);
  5. the self-check: the kit's own (reachability, floating blocks, light, every marker) plus what only this build has:
     nothing opens into the hollow inside of the berg, the canal loop is boat-wide and two deep with two clear blocks
     above it, the sea is a closed basin, the sled run only ever steps down, Appa's spot is free, you can walk on board
     Zhao's ship and down into its hold, and the islet of the ice bridge and the cage of the moon fish stay out of reach.
"""
from collections import deque

import features.einde2_kit as kit
from features import einde2_bouw_water_grond as g
from features import einde2_bouw_water_schip as schip
from features import einde2_bouw_water_stad as stad
from features import guheinde_bouw as gb
from features.einde2_bouw_water_grond import D, H, KANSEL, MUUR, MUUR_Z, MX, P0, SEA, T0, T1, T2, W

NAAM = "noordelijke_waterstam"
WERELD_Y = 142               # the world y of template y 0 (the sea lies at 172, the palace spire at 212)
START = (126, T0 + 1, 159)   # aankomst_speler
LAMPEN = {stad.LANTAARN: 15, schip.GLOED: 15}
WATER = "minecraft:water"
# the floors verlicht() may turn into a lamp, and which lamp
VLOER = {g.IJSSTEEN: stad.LANTAARN, "minecraft:packed_ice": stad.LANTAARN, "minecraft:blue_ice": stad.LANTAARN, "minecraft:snow_block": stad.LANTAARN,
         stad.TEGEL: stad.LANTAARN, "minecraft:spruce_planks": stad.LANTAARN, "minecraft:white_concrete": stad.LANTAARN,
         "minecraft:light_blue_concrete": stad.LANTAARN, "minecraft:light_blue_terracotta": stad.LANTAARN, "minecraft:moss_block": "minecraft:verdant_froglight",
         "minecraft:stone": "minecraft:ochre_froglight", "minecraft:sand": "minecraft:sea_lantern", schip.STENEN: schip.GLOED,
         schip.ROOSTER: schip.GLOED, schip.STAAL: schip.GLOED}


class Bouw(kit.Bouw):
    """kit.Bouw for a build of snow and light blue: the kit's list of thin things you walk through names "minecraft:snow"
    (the layers) and "minecraft:light" (the light block), and that is also how the names of a snow BLOCK and of every light
    blue or light gray block start. Here those are what they are: solid, unless guheinde_bouw calls them thin (a carpet)."""

    def is_dun(self, n):
        if n == "minecraft:snow_block" or n.startswith("minecraft:light_"):
            return gb.is_thin(n)
        return super().is_dun(n)


def tralies(b):
    """Our own bars (roosterijzer_tralies) connect like iron bars do (kit's connect() only knows the vanilla ones)."""
    blocks = b.s.blocks
    for (x, y, z), (name, props, nbt) in list(blocks.items()):
        if name != schip.TRALIES:
            continue
        p = {"waterlogged": "false"}
        for d, (dx, dz) in gb.DIRS.items():
            c = (x + dx, y, z + dz)
            nb = blocks.get(c)
            p[d] = "true" if nb is not None and (nb[0] == schip.TRALIES or (b.solid_at(c) and "door" not in nb[0])) else "false"
        blocks[(x, y, z)] = (name, p, nbt)


# =====================================================================================================================
# markers
# =====================================================================================================================
def plekken(b, gr, haltes, sp):
    z0, z1 = MUUR_Z
    # --- standard: where you arrive, the camp, the view, the rest points ---
    ax0, ax1, az0, az1 = stad.AANKOMST
    b.zone("aankomst", ax0, T0 + 1, az0, ax1, T0 + 10, az1, wie="het landingsijs buiten de poort")
    b.prop("aankomst_speler", *START)
    b.prop("uitzicht", MX, 66, D + 28, wie="boven de zee, recht voor de ijsmuur")
    kx0, kx1, kz0, kz1 = stad.KAMP
    b.zone("kamp", kx0, T0 + 1, kz0, kx1, T0 + 6, kz1, wie="het kamp van Team Guhvatar")
    b.prop("appa_kamp", 145, T0 + 1, 162, wie="Appa-guh ligt hier (5 x 5 vrij)")
    fx, fz = stad.VUUR
    b.plek("team_aang", fx, T0 + 1, fz - 2, yaw=0.0, wie="Aang-guh", kind="AANG_GUH")
    b.plek("team_momo", fx + 2, T0 + 1, fz - 2, yaw=20.0, wie="Momo-guh")
    b.plek("team_katara", fx - 2, T0 + 1, fz + 2, yaw=-135.0, wie="Katara-guh", kind="KATARA_GUH")
    b.plek("team_sokka", fx + 2, T0 + 1, fz + 2, yaw=135.0, wie="Sokka-guh", kind="SOKKA_GUH")
    b.plek("team_toph", fx + 4, T0 + 1, fz, yaw=90.0, wie="Toph-guh", kind="TOPH_GUH")
    b.plek("team_zuka", fx - 4, T0 + 1, fz + 3, yaw=-90.0, wie="Zuka", kind="ZUKA")
    rust = [(134, T0 + 1, 154, "bij het kamp"), (170, T0 + 1, 154, "op de haven, voor het sluipen naar het schip"),
            (118, T0 + 1, 134, "binnen de poort"), (MX + 4, T2 + 1, 36, "achter het paleis, voor de geestenoase"),
            (90, MUUR + 1, z0 + 5, "op de ijsmuur, bij de sleebaan en de Golfkansel")]
    for i, (x, y, z, wie) in enumerate(rust, 1):
        stad.rustvuur(b, x + 1, y, z)
        b.prop(f"rustpunt_{i}", x, y, z, wie=wie)
    # --- the ice wall ---
    b.prop("ijsmuur_poort", MX, T0 + 1, z0 + 5, wie="het midden van de poortboog (hier sluit de poort)")
    b.plek("poortwachter", 117, T0 + 1, z1 + 6, yaw=0.0, wie="de poortwachter-guh", kind="WATERSTAM_WACHTER")
    b.zone("ijsmuur_top", g.MUUR_X[0], MUUR + 1, z0 + 1, g.MUUR_X[1], MUUR + 8, z1 - 1, wie="de weergang op de ijsmuur")
    b.prop("golf_plek", MX, KANSEL + 1, z1 - 1, wie="op de Golfkansel: hier sta je voor de grote golf")
    # --- the city ---
    yw = SEA
    n, zz, ww, oo = (g.RING_N[2] + g.RING_N[3]) // 2, (g.RING_Z[2] + g.RING_Z[3]) // 2, (g.RING_W[0] + g.RING_W[1]) // 2, (g.RING_O[0] + g.RING_O[1]) // 2
    b.pad("gracht_route", [(MX, yw, n), (oo, yw, n), (oo, yw, zz), (MX, yw, zz), (ww, yw, zz), (ww, yw, n), (MX, yw, n)],
          wie="de ringgracht, een rondje met de klok mee vanaf het bekken")
    b.pad("gracht_groot", [(MX, yw, n), (MX, yw, zz), (MX, yw, z0 + 5), (MX, yw, 166)], wie="de Grote Gracht: van het bekken door de poort naar zee")
    b.pad("gracht_dwars", [(ww, yw, 117), (MX, yw, 117), (oo, yw, 117)], wie="de dwarsgracht tussen de huizen (smal: drie breed)")
    b.prop("gondel_start", *haltes[0], wie="de kade aan het bekken, onder aan de grote trap")
    for i, c in enumerate(haltes[1:], 1):
        b.prop(f"gondel_halte_{i}", *c, wie=["bij de poort", "aan de westgracht", "aan de oostgracht"][i - 1])
    bewoners = [(70, T0 + 1, 109, 0.0, "bij de huizen aan de noordkade"), (150, T0 + 1, 109, 0.0, "bij de huizen aan de noordkade"),
                (86, T0 + 1, 126, 180.0, "op de zuidkade"), (140, T0 + 1, 125, 180.0, "op de zuidkade"), (100, T0 + 1, 109, 90.0, "op de markt"),
                (68, T1 + 1, 87, -90.0, "in de bovenstraat"), (159, T0 + 1, 160, 180.0, "in de haven"), (84, T2 + 1, 66, 180.0, "op het paleisterras")]
    for i, (x, y, z, yaw, wie) in enumerate(bewoners, 1):
        b.plek(f"bewoner_{i}", x, y, z, yaw=yaw, wie="een Waterstam-guh " + wie, kind="WATERSTAM_GUH")
    b.zone("markt", 96, T0 + 1, 108, 106, T0 + 5, 115, wie="de markt aan het bekken")
    # --- the training square ---
    px0, px1, pz0, pz1 = stad.PLEIN
    b.zone("trainingsplein", px0, T1 + 1, pz0, px1, T1 + 8, pz1, wie="het trainingsplein van Meester Pakguh")
    b.plek("pakguh", MX, T1 + 1, pz0 + 2, yaw=0.0, wie="Meester Pakguh", kind="PAKGUH")
    b.plek("katara", MX - 3, T1 + 1, pz0 + 8, yaw=180.0, wie="Katara-guh", kind="KATARA_GUH")
    b.plek("sokka", MX + 6, T1 + 1, pz0 + 13, yaw=150.0, wie="Sokka-guh", kind="SOKKA_GUH")
    for i, (x, z) in enumerate(stad.DOELEN, 1):
        b.prop(f"zweep_doel_{i}", x, T1 + 2, z, wie="een zweepdoel")
    bx0, bx1, bz0, bz1 = g.BASSIN
    b.zone("golf_bassin", bx0, T1 - 2, bz0, bx1, T1 + 6, bz1, wie="het golfbassin")
    b.prop("ijsbrug_a", g.PIER[1], T1, g.PIER[2] + 1, wie="het eind van de steiger")
    b.prop("ijsbrug_b", int(g.EILANDJE[0] - g.EILANDJE[2]) + 1, T1, g.EILANDJE[1], wie="de rand van het eilandje", los=True)
    # --- the palace ---
    x0, x1, pz0_, pz1_ = g.PALEIS
    b.zone("paleis", x0, P0, pz0_, x1, P0 + 38, pz1_, wie="het paleis van de hoofdman")
    b.plek("hoofdman", MX, P0 + 3, pz0_ + 4, yaw=0.0, wie="hoofdman Arnook-guh", kind="ARNOOK_GUH")
    b.plek("yue", MX + 3, P0 + 3, pz0_ + 4, yaw=0.0, wie="prinses Yue-guh", kind="YUE_GUH")
    b.prop("troon", MX, P0 + 3, pz0_ + 2, wie="de zetel van de hoofdman")
    b.prop("paleis_poort", MX, P0 + 1, pz1_, wie="de grote deur van het paleis")
    b.zone("genezershut", 69, T2 + 1, 45, 79, T2 + 6, 55, wie="de genezershut van Yugoda-guh")
    # --- the spirit oasis ---
    cx, cz, straal = g.OASE
    vx, vz, vr = g.VIJVER
    b.zone("geestenoase", int(cx - straal), T2 - 1, int(cz - straal), int(cx + straal), T2 + 14, int(cz + straal), wie="de geestenoase")
    b.prop("oase_poort", MX, T2 + 1, g.STAD2[2] - 1, wie="het deurtje in het ijs")
    b.prop("vis_maan", vx - 3, T2, vz, wie="hier zwemt Tui, de maanvis")
    b.prop("vis_oceaan", vx + 3, T2, vz, wie="hier zwemt La, de zeevis")
    b.prop("yue_maan", vx, T2 + 1, vz, wie="op het eilandje in de vijver, onder het poortje: hier wordt Yue-guh de maan")
    # --- the sled run ---
    b.prop("slee_start", 96, MUUR + 1, z1 + 4, wie="de Glijkansel op de ijsmuur")
    b.pad("slee_baan", gr.baan_lijn, wie="de sleebaan, van de muur naar het plat")
    b.prop("slee_finish", 90, T0 + 1, 163, wie="voor de sneeuwhoop")
    for i, (x, z) in enumerate(((96, z1 + 2), (97, z1 + 5), (95, z1 + 6), (97, z1 + 3)), 1):
        b.wezen(f"otter_{i}", x, MUUR + 1, z, "guhs:otter_pinguinguh", yaw=90.0, honk=4)
    for i, (x, z) in enumerate(((68, 156), (76, 159), (84, 157)), 5):
        b.wezen(f"otter_{i}", x, T0 + 1, z, "guhs:otter_pinguinguh", yaw=180.0, honk=8)
    # --- the harbour and Zhao's ship ---
    sx0, sx1, sz0, sz1 = g.SCHIP
    b.zone("haven", 150, T0 + 1, 150, 206, T0 + 14, 206, wie="de haven buiten de muur")
    b.zone("zhao_schip", sx0 - 1, schip.KIEL, sz0 - 3, sx1 + 1, schip.DEK + 18, sz1, wie="de VNS Schroeiplek")
    b.plek("zhao", *sp["brug"], yaw=180.0, wie="admiraal Zhao-Mika, op de brug", kind="ZHAO_MIKA")
    b.prop("sluip_start", 170, T0 + 1, 157, wie="achter de tonnen op de haven")
    b.pad("sluip_route", [(170, T0 + 1, 157), (174, T0 + 1, 162), (174, T0 + 1, 186), sp["ladder"], (sx0 + 1, schip.DEK + 1, 186), (sx0 + 1, schip.DEK + 1, 196),
                          sp["dek_achter"], sp["luik"], sp["trap_onder"], sp["celdeur"]],
          wie="over de steiger, de ladder op, achter de tonnen langs om de schoorsteen, de trap achter de toren af en naar de cel")
    wachten = [(sp["plank"], 180.0, "boven aan de boegklep"), (sp["dek_voor"], 180.0, "op het voordek, bij de katapult"), (sp["dek_toren"], 180.0, "voor de deur van de toren"),
               (sp["ruim"], 0.0, "in het ruim"), ((sp["celdeur"][0], sp["celdeur"][1], sp["celdeur"][2] - 2), 180.0, "voor de cel")]
    for i, (c, yaw, wie) in enumerate(wachten, 1):
        b.plek(f"sluip_wacht_{i}", *c, yaw=yaw, wie="een Vuurnatie-Mika " + wie, kind="VUURNATIE_MIKA")
    b.prop("maanvis_kooi", *sp["kooi"], wie="de kuip in de kooi, in de cel in het achterschip", los=True)
    vx0, vx1, vz0, vz1 = g.VLOOT
    b.zone("vloot", vx0, SEA + 1, vz0, vx1, SEA + 24, vz1, wie="open zee voor de muur: de vloot, en de grote golf")
    # --- the winks ---
    b.pad("knipoog_sjokkel", [(84, T2 + 2, 75), (96, T2 + 2, 75)], wie="Sjokkel kruipt over de rand van het paleisterras, achter het trainingsplein")
    b.los.add("knipoog_sjokkel")
    b.camera("knipoog_sjokkel", (99, T1 + 3, 86), (90, T2 + 2, 75), wie="vanaf het plein omhoog naar de rand")
    b.pad("knipoog_balto_route", [(52, T1 + 1, 89), (80, T1 + 1, 89), (98, T1 + 1, 89), (98, T1 + 1, 91), (56, T1 + 1, 91)],
          wie="Baltoguh sjokt met zijn slee de bovenstraat uit het plein op en weer terug")
    b.plek("knipoog_balto_stop", 98, T1 + 1, 89, yaw=180.0, wie="Baltoguh blijft staan en kijkt naar de ijsstad")
    b.camera("knipoog_balto", (104, T1 + 4, 96), (100, T1 + 2, 87), zicht=["knipoog_balto_stop", "pakguh"], wie="vanaf de grote trap over het plein")


VERWACHT = ["ijsmuur_poort", "poortwachter", "ijsmuur_top", "gracht_route", "gondel_start", ("gondel_halte", 3), ("bewoner", 6), "trainingsplein",
            "pakguh", "katara", "sokka", ("zweep_doel", 5), "golf_bassin", "ijsbrug_a", "ijsbrug_b", "paleis", "hoofdman", "yue", "geestenoase",
            "oase_poort", "vis_maan", "vis_oceaan", "yue_maan", "slee_start", "slee_baan", "slee_finish", ("otter", 4), "haven", "zhao_schip", "zhao",
            "sluip_start", "sluip_route", ("sluip_wacht", 5), "maanvis_kooi", "golf_plek", "vloot", "vloot_anker", "knipoog_sjokkel",
            "knipoog_sjokkel_camera", "knipoog_balto_route", "knipoog_balto_stop", "knipoog_balto_camera"]


# =====================================================================================================================
# light
# =====================================================================================================================
def _dicht(b, c):
    bl = b.s.blocks.get(c)
    if bl is None:
        return False
    n = bl[0]
    return n != gb.AIR and n not in kit.WATER and not b.is_dun(n) and not any(t in n for t in gb.TRANSPARENT_SUB) and n not in kit.LICHT


def rooster(b, gr):
    """Lamps where a builder would lay them: an ice lantern in the paving every eight blocks on the open ground of the three
    tiers and the shelf, and a sea lantern in the bed of the sea every seven. verlicht() then only fills what is left."""
    blocks = b.s.blocks
    for x in range(4, W, 8):
        for z in range(4, D, 8):
            y = int(gr.G[x, z])
            if y in (T0, T1, T2) and (gr.kom[x, z] or gr.plaat[x, z]) and (x, y + 1, z) not in blocks and \
                    blocks.get((x, y, z), ("",))[0] in (g.IJSSTEEN, "minecraft:snow_block"):
                blocks[(x, y, z)] = (stad.LANTAARN, {}, None)
    for x in range(3, W, 7):
        for z in range(3, D, 7):
            y = int(gr.G[x, z])
            if gr.zee[x, z] and blocks.get((x, y + 1, z), ("",))[0] == WATER and blocks.get((x, y, z), ("",))[0] != WATER:
                blocks[(x, y, z)] = ("minecraft:sea_lantern", {}, None)


def verlicht(b, start, min_licht=6):
    """Lays a lamp in the floor under every walkable spot that is still darker than min_licht (the self-check wants 4: two
    to spare). It takes the darkest neighbourhood first and the spot in its middle, so the lamps come to lie apart.
    Returns how many lamps it laid."""
    reach = kit.Loper(b).bfs(start)
    light = b.licht()
    blocks = b.s.blocks
    emmer = {}
    for c in reach:
        if light.get(c, 0) < min_licht:
            emmer.setdefault((c[0] // 8, c[1] // 5, c[2] // 8), set()).add(c)
    gelegd = 0
    while emmer:
        sleutel = max(emmer, key=lambda k: (len(emmer[k]), k))
        cellen = sorted(emmer[sleutel])
        mx, my, mz = (sum(c[i] for c in cellen) / len(cellen) for i in range(3))
        plek = None
        for c in sorted(cellen, key=lambda c: (abs(c[0] - mx) + abs(c[1] - my) + abs(c[2] - mz), c)):
            x, y, z = c
            while (x, y - 1, z) in blocks and blocks[(x, y - 1, z)][0] in kit.WATER:
                y -= 1
            onder = blocks.get((x, y - 1, z))
            if onder is not None and onder[0] in VLOER:
                plek = ((x, y - 1, z), VLOER[onder[0]] if blocks.get((x, y, z), ("",))[0] not in kit.WATER else "minecraft:sea_lantern")
                break
        if plek is None:
            # (they stand on furniture or stairs: the nearest real floor within a few steps through the air)
            gezien, rij = {cellen[0]}, deque([(cellen[0], 0)])
            while rij and plek is None:
                (x, y, z), n = rij.popleft()
                onder = blocks.get((x, y - 1, z))
                if onder is not None and onder[0] in VLOER and blocks.get((x, y, z), ("",))[0] not in kit.WATER:
                    plek = ((x, y - 1, z), VLOER[onder[0]])
                elif n < 9:
                    for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, -1, 0), (0, 1, 0)):
                        nb = (x + dx, y + dy, z + dz)
                        if nb not in gezien and b.inside(*nb) and not _dicht(b, nb):
                            gezien.add(nb)
                            rij.append((nb, n + 1))
        if plek is None:
            raise SystemExit(f"{NAAM}: geen vloer voor een lamp bij {cellen[:4]} (onder: {[blocks.get((c[0], c[1] - 1, c[2]), ('niets',))[0] for c in cellen[:4]]})")
        pos, naam = plek
        blocks[pos] = (gb.mc(naam), {"axis": "y"} if "froglight" in naam else {}, None)
        gelegd += 1
        todo = deque([pos])
        light[pos] = 15
        while todo:
            c = todo.popleft()
            l = light[c] - 1
            if l <= 0:
                continue
            x, y, z = c
            for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                nb = (x + dx, y + dy, z + dz)
                if b.inside(*nb) and light.get(nb, 0) < l and not _dicht(b, nb):
                    light[nb] = l
                    todo.append(nb)
                    if l >= min_licht:
                        k = (nb[0] // 8, nb[1] // 5, nb[2] // 8)
                        if k in emmer and nb in emmer[k]:
                            emmer[k].discard(nb)
                            if not emmer[k]:
                                del emmer[k]
    return gelegd


# =====================================================================================================================
# the checks only this build has
# =====================================================================================================================
def eigen_controle(b, gr, reach):
    p = []
    # what must stay out of reach until the story opens it: the islet in the wave pool (an ice bridge), the tub in the cage
    ex, ez, _ = g.EILANDJE
    if (int(ex), T1 + 1, int(ez)) in reach:
        p.append("het eilandje in het golfbassin is zonder ijsbrug te bereiken")
    kooi = tuple(b.plekken["maanvis_kooi"]["pos"])
    if kooi in reach or gb.near(reach, kooi):
        p.append("de kuip in de kooi is te bereiken")
    for id in ("zhao", "sluip_wacht_4", "sluip_wacht_5", "hoofdman", "yue_maan", "golf_plek", "slee_start"):
        if tuple(b.plekken[id]["pos"]) not in reach:
            p.append(f"{id}: daar kom je niet")
    blocks = b.s.blocks
    naam = lambda c: (blocks.get(c) or (None,))[0]  # noqa: E731
    # nothing opens into the hollow inside of the berg
    w = kit.Loper(b)
    gat = []
    for (x, y, z), bl in blocks.items():
        if not (bl[0] == gb.AIR or w.passable((x, y, z))):
            continue
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            nb = (x + dx, y + dy, z + dz)
            if b.inside(*nb) and nb not in blocks and gr.HOL[nb]:
                gat.append(nb)
    if gat:
        p.append(f"{len(gat)} gaten naar de holle binnenkant van de berg, bv. {sorted(gat)[:5]}")
    # the sea and the canals are closed basins: every water block has water or something solid beside and under it
    lek = []
    for (x, y, z), bl in blocks.items():
        if bl[0] != WATER:
            continue
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            nb = (x + dx, y + dy, z + dz)
            if bl[1].get("level") == "8" and dy == 0:
                continue                                         # (the waterfall: a falling column has air around it)
            n = naam(nb)
            if n is None or n == gb.AIR or (n != WATER and not b.solid_at(nb) and "slab" not in n and "trapdoor" not in n and "ladder" not in n):
                lek.append(((x, y, z), n))
    if lek:
        p.append(f"{len(lek)} waterblokken kunnen weglopen, bv. {sorted(lek, key=str)[:5]}")
    # the canal routes: three wide, two deep, two clear blocks above (a guh in a boat fits under every bridge)
    for id in ("gracht_route", "gracht_groot", "gracht_dwars"):
        punten = b.plekken[id]["punten"]
        breed = 0 if id == "gracht_dwars" else 1
        for (ax, ay, az), (bx, by, bz) in zip(punten, punten[1:]):
            stappen = max(abs(bx - ax), abs(bz - az))
            for s in range(stappen + 1):
                x, z = ax + (bx - ax) * s // max(1, stappen), az + (bz - az) * s // max(1, stappen)
                for d in range(-1, 2):
                    cx, cz = (x, z + d) if ax != bx else (x + d, z)
                    if abs(d) > max(breed, 1):
                        continue
                    kolom = [naam((cx, SEA - 1, cz)), naam((cx, SEA, cz)), naam((cx, SEA + 1, cz)), naam((cx, SEA + 2, cz))]
                    if kolom[:2] != [WATER, WATER] or any(n not in (None, gb.AIR) for n in kolom[2:]):
                        p.append(f"{id}: geen vrije vaart op {(cx, cz)}: {kolom}")
    # the sled run: from the start down to the finish each step is level or one down, three wide all the way
    vorig = None
    for (x, y, z) in gr.baan_lijn:
        if naam((x, y - 1, z)) is None or naam((x, y, z)) not in (None, gb.AIR) or naam((x, y + 1, z)) not in (None, gb.AIR):
            p.append(f"slee_baan: {(x, y, z)} ligt niet vrij op de baan ({naam((x, y - 1, z))} / {naam((x, y, z))})")
        if vorig is not None and not (0 <= vorig - y <= 3):
            p.append(f"slee_baan: van {vorig} naar {y} bij {(x, z)} gaat niet rustig omlaag")
        vorig = y
    tops = {}
    for (x, z), (top, soort) in gr.baan.items():
        if soort == "baan":
            tops[(x, z)] = top
    for (x, z), top in tops.items():
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            t2 = tops.get((x + dx, z + dz))
            if t2 is not None and abs(t2 - top) > 1:
                p.append(f"slee_baan: een stap van {abs(t2 - top)} tussen {(x, z)} en {(x + dx, z + dz)}")
    # Baltoguh's way onto the square: three wide, level, nothing in it
    w = kit.Loper(b)
    punten = b.plekken["knipoog_balto_route"]["punten"]
    for (ax, ay, az), (bx, by, bz) in zip(punten, punten[1:]):
        stappen = max(abs(bx - ax), abs(bz - az))
        for s in range(stappen + 1):
            x, z = ax + (bx - ax) * s // max(1, stappen), az + (bz - az) * s // max(1, stappen)
            for d in (-1, 0, 1):
                c = (x, ay, z + d) if ax != bx else (x + d, ay, z)
                if ay != by or not w.standable(c) or naam(c) is not None or naam((c[0], c[1] + 1, c[2])) is not None:
                    p.append(f"knipoog_balto_route: niet vrij en vlak op {c}: {naam(c)}")
    # Appa's spot: 5 x 5 free, three high
    ax, ay, az = b.plekken["appa_kamp"]["pos"]
    for x in range(ax - 2, ax + 3):
        for z in range(az - 2, az + 3):
            for y in range(ay, ay + 3):
                if naam((x, y, z)) not in (None, gb.AIR):
                    p.append(f"appa_kamp: {naam((x, y, z))} op {(x, y, z)}")
    # the fleet's water is open, and the overlay fits in it
    vx0, _, vz0 = b.plekken["vloot"]["pos"]
    vx1, _, vz1 = b.plekken["vloot"]["tot"]
    if vx1 - vx0 + 1 < 96 or vz1 - vz0 + 1 < 48:
        p.append("vloot: kleiner dan 96 x 48")
    for x in range(vx0, vx1 + 1):
        for z in range(vz0, vz1 + 1):
            if naam((x, SEA, z)) != WATER or any(naam((x, y, z)) not in (None, gb.AIR) for y in range(SEA + 1, SEA + 20)):
                p.append(f"vloot: geen open water op {(x, z)}")
                break
    laag = b.lagen.get("vloot")
    if laag:
        (lx, ly, lz), (sx, sy, sz) = laag["pos"], laag["grootte"]
        if not (vx0 <= lx and lx + sx - 1 <= vx1 and vz0 <= lz and lz + sz - 1 <= vz1 and ly == SEA + 1):
            p.append(f"laag_vloot ligt niet in de vlootzone op het water: {laag}")
    return p[:60]


# =====================================================================================================================
def bouw(h, opslaan=True, streng=True):
    """The whole build -> (b, gr). Raises SystemExit when the self-check finds anything (streng=False, for a look at a build
    in the making: the problems are in b.problemen instead)."""
    kit.LICHT.update(LAMPEN)
    gr = g.Grond()
    b = Bouw(h, NAAM, (W, H, D), kit.STRUCTUREN[NAAM]["salt"] + 50, prefix="sign.guhs.einde2_bouw_water")
    gr.emit(b)
    b.gr = gr
    r = b.rng
    stad.muur(b)
    stad.tiertrappen(b)
    stad.bruggen(b)
    stad.benedenstad(b, r)
    haltes = stad.gondelhaltes(b)
    stad.middenstad(b, r)
    stad.bovenstad(b, r)
    stad.oase(b, r)
    stad.sleebaan(b, gr.baan)
    stad.kamp(b)
    stad.haven(b)
    sp = schip.vlaggenschip(b)
    plekken(b, gr, haltes, sp)
    vx0, vz0 = g.VLOOT[0] + 2, g.VLOOT[2] + 2
    b.laag("vloot", schip.vloot(h), vx0, SEA + 1, vz0)
    b.prop("vloot_anker", vx0, SEA + 1, vz0, wie="de oorsprong van laag_vloot (de noordwesthoek, op het water)")
    b.connect()
    tralies(b)
    rooster(b, gr)
    b.lampen = verlicht(b, START)
    problems = b.check(START, VERWACHT) + eigen_controle(b, gr, kit.Loper(b).bfs(START))
    b.problemen = problems
    if problems and streng:
        raise SystemExit(f"{NAAM}: self-check found {len(problems)} problem(s):\n  " + "\n  ".join(problems[:60]))
    if opslaan:
        b.save_tiles()
    return b, gr
