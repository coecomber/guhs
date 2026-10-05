"""
Among Guhs: the ship "De Vadsvaarder" (arena template guhs:guhpixel/among_vadsvaarder) and its layout file.

The ship is nine rooms on a 3 x 3 grid with corridors between them (top view, x to the east, z to the south):

    Slaapzaal ==== Kantine ==== Navigatie
        |             |             |
     Reactor   Ziekenboeg-+-Elektra |
        |             |             |
    Machinekamer = Voorraadkamer = Schildkamer

Everything the game needs to know about the ship is written to data/guhs/guhpixel/among_schip.txt (a plain text table,
read by Java: feature/guhpixel/among/model/Schip): the zones (rooms and corridors), the walking graph of the guh NPCs,
the doors, the task panels, the vents with their networks, the emergency button, the meeting spots, the beds and the
task list. This module is the ONLY source of those numbers: change the ship here and Java follows.

Coordinates are template cells; the floor is y 1, feet stand on y 2 (VOET), the ceiling is y 6.
"""
import json

NAME = "guhpixel/among_vadsvaarder"
W, H, D = 75, 9, 51
VOET = 2
PLAFOND = 6

PANEEL, KNOP, LUIK = "guhs:among_taakpaneel", "guhs:among_noodknop", "guhs:among_ventilatieluik"

# id: (x0, z0, x1, z1, floor block, floor accent)
KAMERS = {
    "slaapzaal": (4, 4, 18, 14, "minecraft:pink_wool", "minecraft:magenta_wool"),
    "kantine": (28, 3, 46, 15, "minecraft:yellow_concrete", "minecraft:white_concrete"),
    "navigatie": (56, 4, 70, 14, "minecraft:light_blue_concrete", "minecraft:white_concrete"),
    "reactor": (4, 20, 18, 30, "minecraft:lime_concrete", "minecraft:gray_concrete"),
    "ziekenboeg": (24, 20, 34, 30, "minecraft:white_concrete", "minecraft:light_gray_concrete"),
    "elektra": (40, 20, 50, 30, "minecraft:yellow_terracotta", "minecraft:gray_concrete"),
    "machinekamer": (4, 36, 18, 46, "minecraft:gray_concrete", "minecraft:light_gray_concrete"),
    "voorraadkamer": (28, 35, 46, 47, "minecraft:orange_terracotta", "minecraft:brown_terracotta"),
    "schildkamer": (56, 36, 70, 46, "minecraft:cyan_concrete", "minecraft:white_concrete"),
}
# id: (x0, z0, x1, z1) including the door cells at both ends
GANGEN = {
    "gang_h1": (19, 8, 27, 10), "gang_h2": (47, 8, 55, 10), "gang_h3": (19, 40, 27, 42), "gang_h4": (47, 40, 55, 42),
    "gang_v1a": (10, 15, 12, 19), "gang_v1b": (10, 31, 12, 35), "gang_v2": (62, 15, 64, 35), "gang_vm": (35, 16, 39, 34),
}
# the middle corridor is 3 wide (x 36..38); its zone is 5 wide because the two side doors (x 35 and x 39) belong to it
VM_BINNEN = (36, 16, 38, 34)

# door id: (room, cells x0, z0, x1, z1, node x, z, corridor)
DEUREN = {
    "slaapzaal_o": ("slaapzaal", 19, 8, 19, 10, 19, 9, "gang_h1"),
    "slaapzaal_z": ("slaapzaal", 10, 15, 12, 15, 11, 15, "gang_v1a"),
    "kantine_w": ("kantine", 27, 8, 27, 10, 27, 9, "gang_h1"),
    "kantine_o": ("kantine", 47, 8, 47, 10, 47, 9, "gang_h2"),
    "kantine_z": ("kantine", 36, 16, 38, 16, 37, 16, "gang_vm"),
    "navigatie_w": ("navigatie", 55, 8, 55, 10, 55, 9, "gang_h2"),
    "navigatie_z": ("navigatie", 62, 15, 64, 15, 63, 15, "gang_v2"),
    "reactor_n": ("reactor", 10, 19, 12, 19, 11, 19, "gang_v1a"),
    "reactor_z": ("reactor", 10, 31, 12, 31, 11, 31, "gang_v1b"),
    "ziekenboeg_o": ("ziekenboeg", 35, 24, 35, 26, 35, 25, "gang_vm"),
    "elektra_w": ("elektra", 39, 24, 39, 26, 39, 25, "gang_vm"),
    "machinekamer_n": ("machinekamer", 10, 35, 12, 35, 11, 35, "gang_v1b"),
    "machinekamer_o": ("machinekamer", 19, 40, 19, 42, 19, 41, "gang_h3"),
    "voorraadkamer_w": ("voorraadkamer", 27, 40, 27, 42, 27, 41, "gang_h3"),
    "voorraadkamer_o": ("voorraadkamer", 47, 40, 47, 42, 47, 41, "gang_h4"),
    "voorraadkamer_n": ("voorraadkamer", 36, 34, 38, 34, 37, 34, "gang_vm"),
    "schildkamer_w": ("schildkamer", 55, 40, 55, 42, 55, 41, "gang_h4"),
    "schildkamer_n": ("schildkamer", 62, 35, 64, 35, 63, 35, "gang_v2"),
}
# corridor edges between door nodes (and the junction of the middle corridor)
GANG_RANDEN = [
    ("deur_slaapzaal_o", "deur_kantine_w"), ("deur_kantine_o", "deur_navigatie_w"),
    ("deur_machinekamer_o", "deur_voorraadkamer_w"), ("deur_voorraadkamer_o", "deur_schildkamer_w"),
    ("deur_slaapzaal_z", "deur_reactor_n"), ("deur_reactor_z", "deur_machinekamer_n"),
    ("deur_navigatie_z", "deur_schildkamer_n"),
    ("deur_kantine_z", "vm_midden"), ("vm_midden", "deur_voorraadkamer_n"),
    ("vm_midden", "deur_ziekenboeg_o"), ("vm_midden", "deur_elektra_w"),
]
# room hubs (every point of a room is reached through a hub); the Kantine has four, around the table
HUBS = {
    "slaapzaal": [(11, 9)], "navigatie": [(63, 9)], "reactor": [(11, 25)], "ziekenboeg": [(29, 25)], "elektra": [(45, 25)],
    "machinekamer": [(11, 41)], "voorraadkamer": [(37, 41)], "schildkamer": [(63, 41)],
    "kantine": [(32, 9), (37, 5), (42, 9), (37, 13)],
}
TAFEL = (36, 8, 38, 10)          # the meeting table in the Kantine (the button stands on its middle)
KNOP_POS = (37, 3, 9)
KNOP_STAAN = (37, 6)

# panel id: (room, wall side, coordinate along that wall); the panel sits IN the wall at y 3 and looks into the room
PANELEN = {
    "slaap_kruimel": ("slaapzaal", "w", 6), "slaap_droom": ("slaapzaal", "w", 12),
    "kantine_kruimel": ("kantine", "n", 31), "kantine_droom": ("kantine", "n", 43),
    "nav_pasje": ("navigatie", "n", 59), "nav_droom": ("navigatie", "n", 67), "nav_worst": ("navigatie", "z", 68),
    "nav_alarm": ("navigatie", "o", 9),
    "reactor_schakel": ("reactor", "w", 23), "reactor_alarm": ("reactor", "w", 27),
    "zieken_weeg": ("ziekenboeg", "w", 23), "zieken_sorteer": ("ziekenboeg", "n", 31),
    "elektra_worst": ("elektra", "n", 43), "elektra_schakel": ("elektra", "o", 23), "elektra_licht": ("elektra", "o", 27),
    "machine_tank": ("machinekamer", "w", 39),
    "voorraad_tank": ("voorraadkamer", "z", 31), "voorraad_sorteer": ("voorraadkamer", "z", 37), "voorraad_worst": ("voorraadkamer", "z", 43),
    "schild_schakel": ("schildkamer", "o", 39), "schild_droom": ("schildkamer", "z", 59),
}
LICHT_PANEEL = "elektra_licht"
ALARM_PANELEN = ("reactor_alarm", "nav_alarm")

# vent id: (room, network, x, z)
LUIKEN = {
    "luik_slaapzaal": ("slaapzaal", "a", 16, 12), "luik_reactor": ("reactor", "a", 16, 22), "luik_machinekamer": ("machinekamer", "a", 16, 45),
    "luik_ziekenboeg": ("ziekenboeg", "b", 26, 29), "luik_elektra": ("elektra", "b", 48, 29), "luik_voorraadkamer": ("voorraadkamer", "b", 45, 36),
    "luik_navigatie": ("navigatie", "c", 69, 12), "luik_schildkamer": ("schildkamer", "c", 69, 38),
}

# task id: (kind, ticks an NPC needs per step, panels in order). The kinds are the eight classics of DESIGN_PX section 3.
TAKEN = {
    "worst_elektra": ("worstjes", 160, ["elektra_worst"]),
    "worst_nav": ("worstjes", 160, ["nav_worst"]),
    "worst_voorraad": ("worstjes", 160, ["voorraad_worst"]),
    "pasje_nav": ("pasje", 120, ["nav_pasje"]),
    "kruimel_kantine": ("kruimelbak", 180, ["kantine_kruimel"]),
    "kruimel_slaap": ("kruimelbak", 180, ["slaap_kruimel"]),
    "tank": ("pindasaus", 160, ["voorraad_tank", "machine_tank"]),
    "sorteer_voorraad": ("sorteren", 200, ["voorraad_sorteer"]),
    "sorteer_zieken": ("sorteren", 200, ["zieken_sorteer"]),
    "droom_slaap": ("dromen", 180, ["slaap_droom", "kantine_droom"]),
    "droom_nav": ("dromen", 180, ["nav_droom", "kantine_droom"]),
    "droom_schild": ("dromen", 180, ["schild_droom", "kantine_droom"]),
    "weeg": ("wegen", 220, ["zieken_weeg"]),
    "schakel_elektra": ("schakelaars", 140, ["elektra_schakel"]),
    "schakel_reactor": ("schakelaars", 140, ["reactor_schakel"]),
    "schakel_schild": ("schakelaars", 140, ["schild_schakel"]),
}
TAAK_SOORTEN = ("worstjes", "pasje", "kruimelbak", "pindasaus", "sorteren", "dromen", "wegen", "schakelaars")

# ten meeting spots on a ring around the table (x, z as cell centres * 2, so halves are possible) and ten beds in the Slaapzaal
STOELEN = [(37.5, 5.5), (40.0, 6.3), (41.5, 8.5), (41.5, 10.5), (40.0, 12.7), (37.5, 13.5), (35.0, 12.7), (33.5, 10.5), (33.5, 8.5), (35.0, 6.3)]
BEDDEN_N = [6, 8, 10, 12, 14, 16]        # head against the north wall
BEDDEN_Z = [5, 7, 15, 17]                # head against the south wall
START = (37.5, 2.0, 12.5)
START_YAW = 180.0

MUUR, STREEP, PLAFOND_BLOK, LAMP = "minecraft:white_concrete", "minecraft:pink_concrete", "minecraft:white_concrete", "minecraft:sea_lantern"
GANG_VLOER, GANG_LIJN = "minecraft:light_gray_concrete", "minecraft:pink_concrete"
ROMP = "minecraft:gray_concrete"


def paneel_plek(pid):
    """(block x, y, z, facing, stand x, stand z) of a panel."""
    kamer, kant, c = PANELEN[pid]
    x0, z0, x1, z1 = KAMERS[kamer][:4]
    if kant == "n":
        return c, 3, z0 - 1, "south", c, z0
    if kant == "z":
        return c, 3, z1 + 1, "north", c, z1
    if kant == "w":
        return x0 - 1, 3, c, "east", x0, c
    return x1 + 1, 3, c, "west", x1, c


def bedden():
    """(x, z of the lying spot, yaw) for the ten beds, in order."""
    x0, z0, x1, z1 = KAMERS["slaapzaal"][:4]
    out = [(x + 0.5, z0 + 1.0, 180.0) for x in BEDDEN_N]
    out += [(x + 0.5, z1 + 0.0, 0.0) for x in BEDDEN_Z]
    return out


def zone_van(x, z):
    for kid, (x0, z0, x1, z1, *_rest) in KAMERS.items():
        if x0 <= x <= x1 and z0 <= z <= z1:
            return kid
    for gid, (x0, z0, x1, z1) in GANGEN.items():
        if x0 <= x <= x1 and z0 <= z <= z1:
            return gid
    return None


def binnen():
    """Every walkable floor cell (x, z) -> its floor block."""
    cellen = {}
    for kid, (x0, z0, x1, z1, vloer, accent) in KAMERS.items():
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                rand = x in (x0, x1) or z in (z0, z1)
                cellen[(x, z)] = accent if rand or (x + z) % 4 == 0 else vloer
    for gid, (x0, z0, x1, z1) in GANGEN.items():
        if gid == "gang_vm":
            x0, z0, x1, z1 = VM_BINNEN
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                midden = (x == (x0 + x1) // 2) if (x1 - x0) < (z1 - z0) else (z == (z0 + z1) // 2)
                cellen[(x, z)] = GANG_LIJN if midden else GANG_VLOER
    for did, (kamer, x0, z0, x1, z1, *_rest) in DEUREN.items():
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                cellen[(x, z)] = GANG_LIJN if (x == (x0 + x1) // 2 and z == (z0 + z1) // 2) else GANG_VLOER
    return cellen


def knopen():
    """The walking graph: ({node: (x, z, zone)}, [(a, b)])."""
    nodes, edges = {}, []
    for kamer, hubs in HUBS.items():
        for i, (x, z) in enumerate(hubs):
            nodes[f"hub_{kamer}_{i}"] = (x, z, kamer)
        for i in range(len(hubs)):
            if len(hubs) > 1:
                edges.append((f"hub_{kamer}_{i}", f"hub_{kamer}_{(i + 1) % len(hubs)}"))
    nodes["vm_midden"] = (37, 25, "gang_vm")
    for did, (kamer, x0, z0, x1, z1, nx, nz, gang) in DEUREN.items():
        nodes[f"deur_{did}"] = (nx, nz, gang)
        edges.append((f"deur_{did}", dichtste_hub(kamer, nx, nz)))
    edges += GANG_RANDEN
    for pid in PANELEN:
        bx, by, bz, facing, sx, sz = paneel_plek(pid)
        kamer = PANELEN[pid][0]
        nodes[f"bij_{pid}"] = (sx, sz, kamer)
        edges.append((f"bij_{pid}", dichtste_hub(kamer, sx, sz)))
    for lid, (kamer, net, x, z) in LUIKEN.items():
        nodes[f"bij_{lid}"] = (x, z, kamer)
        edges.append((f"bij_{lid}", dichtste_hub(kamer, x, z)))
    nodes["bij_knop"] = (KNOP_STAAN[0], KNOP_STAAN[1], "kantine")
    edges.append(("bij_knop", "hub_kantine_1"))
    return nodes, edges


def dichtste_hub(kamer, x, z):
    hubs = HUBS[kamer]
    i = min(range(len(hubs)), key=lambda k: (hubs[k][0] - x) ** 2 + (hubs[k][1] - z) ** 2)
    return f"hub_{kamer}_{i}"


def sign(s, x, y, z, facing, regels):
    """A wall sign with a room name (lang keys sign.guhs.among.<slug>)."""
    import sign_text
    from make_structures import NbtList, Byte
    msgs = sign_text.messages("sign.guhs.among", regels)
    leeg = NbtList(8, [json.dumps({"text": ""})] * 4)
    s.set(x, y, z, "minecraft:cherry_wall_sign", {"facing": facing, "waterlogged": "false"},
          {"id": "minecraft:sign", "front_text": {"messages": msgs, "color": "black", "has_glowing_text": Byte(0)},
           "back_text": {"messages": leeg, "color": "black", "has_glowing_text": Byte(0)}, "is_waxed": Byte(1)})


KAMER_NAMEN = {
    "slaapzaal": "Slaapzaal", "kantine": "Kantine", "navigatie": "Navigatie", "reactor": "Reactor", "ziekenboeg": "Ziekenboeg",
    "elektra": "Elektra", "machinekamer": "Machinekamer", "voorraadkamer": "Voorraadkamer", "schildkamer": "Schildkamer",
}
BORD_GRAP = {
    "slaapzaal": "Stil, njeg!", "kantine": "Eerst knabbelen", "navigatie": "Rechtdoor? Njeg", "reactor": "Op kaaskracht",
    "ziekenboeg": "Dutje helpt", "elektra": "Niet aanzitten", "machinekamer": "Het bromt: goed", "voorraadkamer": "Niet snoepen",
    "schildkamer": "Schild aan?",
}


def bouw(h):
    s = h.Structure((W, H, D))
    cellen = binnen()
    muren = set()
    for (x, z) in cellen:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if (x + dx, z + dz) not in cellen:
                    muren.add((x + dx, z + dz))
    for (x, z), vloer in cellen.items():
        s.set(x, 0, z, ROMP)
        s.set(x, 1, z, vloer)
        for y in range(VOET, PLAFOND):
            s.set(x, y, z, "minecraft:air")
        s.set(x, PLAFOND, z, LAMP if x % 4 == 1 and z % 4 == 1 else PLAFOND_BLOK)
    for (x, z) in muren:
        s.set(x, 0, z, ROMP)
        for y in range(1, PLAFOND + 1):
            s.set(x, y, z, STREEP if y == 2 else MUUR)
    # the doors are 3 high: wall above them
    for did, (kamer, x0, z0, x1, z1, *_rest) in DEUREN.items():
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                s.set(x, 5, z, STREEP)
    # windows: pink glass in the outer walls of the rooms with a view
    uitzicht = ("navigatie", "schildkamer", "kantine", "gang_v2")
    kruis = ((1, 0), (-1, 0), (0, 1), (0, -1))
    for (x, z) in sorted(muren):
        binnen_buren = [(x + dx, z + dz) for dx, dz in kruis if (x + dx, z + dz) in cellen]
        ruimte = sum((x + dx, z + dz) not in cellen and (x + dx, z + dz) not in muren for dx, dz in kruis)
        if len(binnen_buren) == 1 and ruimte == 1 and (x + z) % 5 in (0, 1) and zone_van(*binnen_buren[0]) in uitzicht:
            for y in (3, 4):
                s.set(x, y, z, "minecraft:pink_stained_glass")
    inrichting(s)
    # the things the game uses
    for pid in PANELEN:
        bx, by, bz, facing, sx, sz = paneel_plek(pid)
        s.set(bx, by, bz, PANEEL, {"facing": facing})
    for lid, (kamer, net, x, z) in LUIKEN.items():
        s.set(x, VOET, z, LUIK, {"facing": "north"})
    s.set(KNOP_POS[0], KNOP_POS[1], KNOP_POS[2], KNOP, {"facing": "north"})
    # the room names beside the doors, on the corridor side
    gedaan = set()
    for did, (kamer, x0, z0, x1, z1, nx, nz, gang) in DEUREN.items():
        if kamer in gedaan:
            continue
        gedaan.add(kamer)
        kx0, kz0, kx1, kz1 = KAMERS[kamer][:4]
        if x0 == x1:      # a door in a west/east wall: the sign hangs inside the room next to the door, facing in
            binnen_x = kx1 if x0 > kx1 else kx0
            sign(s, binnen_x, 4, z0 - 1, "west" if x0 > kx1 else "east", ["", KAMER_NAMEN[kamer], BORD_GRAP[kamer], ""])
        else:
            binnen_z = kz1 if z0 > kz1 else kz0
            sign(s, x0 - 1, 4, binnen_z, "north" if z0 > kz1 else "south", ["", KAMER_NAMEN[kamer], BORD_GRAP[kamer], ""])
    controleer(s)
    s.save(NAME)
    return s


def inrichting(s):
    """Furniture and decoration: against the walls, never on a walking spot (the checks below guard that)."""
    # Kantine: the round-ish meeting table with the button, a buffet along the south wall
    x0, z0, x1, z1 = TAFEL
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            s.set(x, VOET, z, "minecraft:smooth_quartz")
    for (x, z) in ((30, 15), (31, 15), (32, 15), (42, 15), (43, 15), (44, 15)):
        s.set(x, VOET, z, "minecraft:barrel", {"facing": "up", "open": "false"})
    s.set(31, VOET + 1, 15, "minecraft:cake", {"bites": "2"})
    s.set(43, VOET + 1, 15, "minecraft:cake", {"bites": "0"})
    for (x, z) in ((28, 3), (46, 3), (28, 15), (46, 15)):
        s.set(x, VOET, z, "minecraft:flowering_azalea")
    # Slaapzaal: ten beds
    kx0, kz0, kx1, kz1 = KAMERS["slaapzaal"][:4]
    for x in BEDDEN_N:
        s.set(x, VOET, kz0, "minecraft:pink_bed", {"facing": "north", "part": "head", "occupied": "false"})
        s.set(x, VOET, kz0 + 1, "minecraft:pink_bed", {"facing": "north", "part": "foot", "occupied": "false"})
    for x in BEDDEN_Z:
        s.set(x, VOET, kz1, "minecraft:magenta_bed", {"facing": "south", "part": "head", "occupied": "false"})
        s.set(x, VOET, kz1 - 1, "minecraft:magenta_bed", {"facing": "south", "part": "foot", "occupied": "false"})
    # Reactor: the kaasreactor (a glowing cheese core behind glass) against the east wall
    for x in range(15, 18):
        for z in range(24, 27):
            for y in range(VOET, VOET + 3):
                kern = (x, z) == (16, 25)
                s.set(x, y, z, "minecraft:shroomlight" if kern else "minecraft:yellow_stained_glass")
    # Machinekamer: two engines against the south wall
    for bx in (6, 13):
        for x in range(bx, bx + 3):
            s.set(x, VOET, 46, "minecraft:blast_furnace", {"facing": "north", "lit": "true"})
            s.set(x, VOET + 1, 46, "minecraft:iron_block")
        s.set(bx + 1, VOET + 2, 46, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    # Voorraadkamer: crates of knabbels in the north corners
    for (x, z, y) in ((28, 35, 0), (29, 35, 0), (28, 36, 0), (28, 35, 1), (46, 35, 0), (46, 37, 0), (44, 35, 0), (46, 35, 1), (28, 47, 0), (46, 47, 0)):
        s.set(x, VOET + y, z, "minecraft:barrel", {"facing": "up", "open": "false"})
    s.set(29, VOET, 36, "minecraft:hay_block", {"axis": "y"})
    # Ziekenboeg: two sick beds against the south wall, a red cross in the floor, the scale in front of its panel
    for x in (30, 33):
        s.set(x, VOET, 30, "minecraft:white_bed", {"facing": "south", "part": "head", "occupied": "false"})
        s.set(x, VOET, 29, "minecraft:white_bed", {"facing": "south", "part": "foot", "occupied": "false"})
    for (dx, dz) in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
        s.set(29 + dx, 1, 22 + dz, "minecraft:red_concrete")
    bx, by, bz, facing, sx, sz = paneel_plek("zieken_weeg")
    s.set(sx, VOET, sz, "minecraft:heavy_weighted_pressure_plate", {"power": "0"})
    # Elektra: fuse boxes against the west wall
    for z in (20, 21, 29, 30):
        s.set(40, VOET, z, "minecraft:redstone_lamp", {"lit": "false"})
        s.set(40, VOET + 1, z, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    # Navigatie: the helm (a lectern with the course) and a star chart against the east wall
    s.set(70, VOET, 5, "minecraft:lectern", {"facing": "west", "has_book": "false", "powered": "false"})
    s.set(70, VOET, 13, "minecraft:cartography_table")
    s.set(70, VOET, 12, "minecraft:cartography_table")
    # Schildkamer: the shield generator (a beacon look-alike) in the north-west corner
    for (x, z) in ((56, 36), (57, 36), (56, 37)):
        s.set(x, VOET, z, "minecraft:light_blue_stained_glass")
    s.set(56, VOET + 1, 36, "minecraft:sea_lantern")


def schip_tekst():
    """The layout table for Java (data/guhs/guhpixel/among_schip.txt)."""
    regels = [f"maat {W} {H} {D}", f"voet {VOET}", f"start {START[0]} {START[1]} {START[2]} {START_YAW}"]
    for kid, (x0, z0, x1, z1, *_rest) in KAMERS.items():
        regels.append(f"zone {kid} kamer {x0} {z0} {x1} {z1}")
    for gid, (x0, z0, x1, z1) in GANGEN.items():
        regels.append(f"zone {gid} gang {x0} {z0} {x1} {z1}")
    nodes, edges = knopen()
    for nid, (x, z, zone) in nodes.items():
        regels.append(f"knoop {nid} {x} {z} {zone}")
    for a, b in edges:
        regels.append(f"rand {a} {b}")
    for did, (kamer, x0, z0, x1, z1, nx, nz, gang) in DEUREN.items():
        regels.append(f"deur {did} {kamer} deur_{did} {x0} {z0} {x1} {z1}")
    for pid, (kamer, kant, c) in PANELEN.items():
        bx, by, bz, facing, sx, sz = paneel_plek(pid)
        soort = "licht" if pid == LICHT_PANEEL else "alarm" if pid in ALARM_PANELEN else "taak"
        regels.append(f"paneel {pid} {kamer} {soort} {bx} {by} {bz} {facing} bij_{pid}")
    for lid, (kamer, net, x, z) in LUIKEN.items():
        regels.append(f"luik {lid} {kamer} {net} {x} {VOET} {z} bij_{lid}")
    regels.append(f"knop {KNOP_POS[0]} {KNOP_POS[1]} {KNOP_POS[2]} bij_knop")
    for i, (x, z) in enumerate(STOELEN):
        regels.append(f"stoel {i} {x} {z}")
    for i, (x, z, yaw) in enumerate(bedden()):
        regels.append(f"bed {i} {x} {z} {yaw}")
    for tid, (soort, duur, panelen) in TAKEN.items():
        regels.append(f"taak {tid} {soort} {duur} " + " ".join(panelen))
    return "\n".join(regels) + "\n"


def controleer(s):
    """The geometry the game relies on."""
    problems = []
    cellen = binnen()
    nodes, edges = knopen()
    vrij = (None, "minecraft:air", LUIK, "minecraft:heavy_weighted_pressure_plate")
    for nid, (x, z, zone) in nodes.items():
        if (x, z) not in cellen:
            problems.append(f"node {nid} at {(x, z)} is not on the floor")
        elif s.get(x, VOET, z) not in vrij or s.get(x, VOET + 1, z) not in (None, "minecraft:air"):
            problems.append(f"node {nid} at {(x, z)} is blocked by {s.get(x, VOET, z)}")
        if zone_van(x, z) != zone and not nid.startswith("deur_"):
            problems.append(f"node {nid}: zone {zone} but the cell lies in {zone_van(x, z)}")
    for a, b in edges:
        if a not in nodes or b not in nodes:
            problems.append(f"edge {a} - {b}: unknown node")
            continue
        (ax, az, _), (bx, bz, _) = nodes[a], nodes[b]
        n = max(abs(bx - ax), abs(bz - az), 1) * 2
        for i in range(n + 1):
            x, z = round(ax + (bx - ax) * i / n), round(az + (bz - az) * i / n)
            if (x, z) not in cellen:
                problems.append(f"edge {a} - {b} leaves the floor at {(x, z)}")
                break
            if s.get(x, VOET, z) not in vrij and i not in (0, n):
                problems.append(f"edge {a} - {b} runs through {s.get(x, VOET, z)} at {(x, z)}")
                break
    # everything reachable from the start
    buren = {}
    for a, b in edges:
        buren.setdefault(a, set()).add(b)
        buren.setdefault(b, set()).add(a)
    gezien, todo = {"hub_kantine_0"}, ["hub_kantine_0"]
    while todo:
        for nb in buren.get(todo.pop(), ()):
            if nb not in gezien:
                gezien.add(nb)
                todo.append(nb)
    for nid in nodes:
        if nid not in gezien:
            problems.append(f"node {nid} cannot be reached")
    for pid in PANELEN:
        bx, by, bz, facing, sx, sz = paneel_plek(pid)
        if (bx, bz) in cellen:
            problems.append(f"panel {pid} is not in a wall")
        if s.get(sx, VOET, sz) not in vrij:
            problems.append(f"panel {pid}: {s.get(sx, VOET, sz)} on its standing spot")
    for tid, (soort, duur, panelen) in TAKEN.items():
        if soort not in TAAK_SOORTEN:
            problems.append(f"task {tid}: unknown kind {soort}")
        for p in panelen:
            if p not in PANELEN or p == LICHT_PANEEL or p in ALARM_PANELEN:
                problems.append(f"task {tid}: {p} is not a task panel")
    for soort in TAAK_SOORTEN:
        if not any(t[0] == soort for t in TAKEN.values()):
            problems.append(f"no task of kind {soort}")
    for (x, z) in STOELEN:
        if s.get(int(x), VOET, int(z)) not in (None, "minecraft:air"):
            problems.append(f"meeting spot {(x, z)} is blocked")
    for (x, y, z), (b, _, _) in s.blocks.items():
        if b in ("minecraft:water", "minecraft:lava"):
            problems.append(f"free {b} at {(x, y, z)}")
    if problems:
        raise SystemExit("among ship check failed:\n  " + "\n  ".join(problems[:40]))


def build(h):
    s = bouw(h)
    with open(f"{h.D}/guhpixel/among_schip.txt", "w", encoding="utf-8", newline="\n") as f:
        f.write(schip_tekst())
    return s
