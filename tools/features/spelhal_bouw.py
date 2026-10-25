"""
1.7.0 playtest fixes: what the five cave-minigame halls share (grotspellen.py: Schatgraven, Glimwormvangen, Lichtjespuzzel;
mijnspellen_bouw.py: the Wisselgrot and the Kegelgrot). Java: feature/spelen/Spelhal.java (the protection) and
world/FlatJigsawStructure.java ("hut_op_grond").

Every game is three templates chained by vertical jigsaws: the hall (the start piece), a shaft of nothing (only two jigsaws)
and the entrance hut on the surface with the top Wolkenliftje straight above the bottom one.

  RAND        a hall template is its hall + RAND blocks of room on all four sides: nothing is stored there except the extra
              entrance tunnels, so the world's rock stays. The Java protects the start piece's box minus RAND sideways (= the
              hall with its own walls), the hut whole; the rock around it and the tunnels can be dug (Spelhal.RAND).
  ingang()    an extra entrance: a door in the hall wall, a short tunnel, a little chamber at the end whose sides are the
              world's own rock (a cave or a digging player runs into it), with signs "Ingang <hall>".
  hut()       the hut template on FUNDERING layers of foundation (dips in the ground under the hut stay filled), its floor
              at y FUNDERING (the block you stand on, level with the ground: its pool element says so, ground_level_delta,
              and the structure puts it on the ground at the hut's own spot: "hut_op_grond").
  verbind()   fences, panes, walls and iron bars get their connection states from their neighbours (a template keeps the
              state it was saved with; nothing updates the shapes when it is placed).
  worldgen()  structure, set, pools and biome tag. start_height = -(FUNDERING + shaft + hall height): with the ground at the
              middle of the hall, the hut's floor lands on it (vanilla: start piece minY + 1 = surface + start_height).
  doorgang()  (checks) can you walk from a spot to another through air, doors and signs?
"""
import json
import os
import re

RAND = 8                     # Java Spelhal.RAND
FUNDERING = 3                # foundation layers under the hut's floor
TUNNEL = 4                   # tunnel cells between the hall wall and the chamber (+ a 3 deep chamber: RAND = TUNNEL + 4)
assert TUNNEL + 4 <= RAND

D = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}
TEGEN = {"north": "south", "south": "north", "west": "east", "east": "west"}
RECHTS = {"north": "east", "east": "south", "south": "west", "west": "north"}   # (clockwise)


def verschuif(ms, S, dx, dy, dz, size):
    """A copy of the template S moved by (dx, dy, dz) in a template of `size`."""
    T = ms.Structure(tuple(size))
    for (x, y, z), b in S.blocks.items():
        T.blocks[(x + dx, y + dy, z + dz)] = b
    for (x, y, z, nbt) in S.entities:
        T.entities.append((x + dx, y + dy, z + dz, nbt))
    return T


# =====================================================================================================================
# an extra entrance
# =====================================================================================================================
def bord_nbt(ms, lines, texts, prefix):
    msgs = []
    for i, line in enumerate((list(lines) + [None] * 4)[:4]):
        if not line:
            msgs.append(json.dumps(""))
            continue
        nl, en = line
        assert len(nl) <= 16 and len(en) <= 16, f"sign line too long: {line}"
        key = f"sign.guhs.{prefix}_{i}"
        texts[key] = (en, nl)
        msgs.append(json.dumps({"translate": key, "fallback": nl}))
    return {"id": "minecraft:sign",
            "front_text": {"messages": ms.NbtList(8, msgs), "color": "black", "has_glowing_text": ms.Byte(0)},
            "back_text": {"messages": ms.NbtList(8, [json.dumps("")] * 4), "color": "black", "has_glowing_text": ms.Byte(0)},
            "is_waxed": ms.Byte(1)}


def ingang(ms, S, x, y, z, richting, *, naam, wie, texts, prefix, vloer, wand, plafond, balk="minecraft:stripped_spruce_log",
           lamp="minecraft:pearlescent_froglight", deur="minecraft:spruce_door", hout="spruce"):
    """An extra entrance from the hall wall cell (x, y, z) (y = the height you stand at: the hall floor block is y - 1) going
    `richting` (out of the hall): a door in the wall, TUNNEL cells of tunnel (1 wide, 2 high, built), a 3 x 3 x 3 chamber
    (floor and ceiling built, its sides and end the world's own rock) with two signs. naam/wie: (Dutch, English) for the
    sign ("Ingang Wisselgrot", "bij Piepwiel"). Returns the chamber's middle (where you stand)."""
    dx, dz = D[richting]
    rx, rz = D[RECHTS[richting]]

    def p(d, s, dy):
        return x + dx * d + rx * s, y + dy, z + dz * d + rz * s
    # the door in the hall wall (facing into the hall: it opens towards the hall)
    for dy, half in ((0, "lower"), (1, "upper")):
        S.set(*p(0, 0, dy), deur, {"facing": TEGEN[richting], "half": half, "hinge": "left", "open": "false", "powered": "false"})
    as_s = "x" if rx else "z"
    for d in range(1, TUNNEL + 1):
        S.set(*p(d, 0, -1), vloer)
        S.set(*p(d, 0, 0), "minecraft:air")
        S.set(*p(d, 0, 1), "minecraft:air")
        frame = d % 2 == 0
        for s in (-1, 1):
            for dy in (-1, 0, 1, 2):
                S.set(*p(d, s, dy), balk if frame and dy in (0, 1) else wand, {"axis": "y"} if frame and dy in (0, 1) else None)
        S.set(*p(d, 0, 2), balk if frame else (lamp if d == TUNNEL - 1 else plafond), {"axis": as_s} if frame else None)
    # the chamber: d TUNNEL+1 .. TUNNEL+3, 3 wide, 3 high
    for d in range(TUNNEL + 1, TUNNEL + 4):
        for s in (-1, 0, 1):
            S.set(*p(d, s, -1), vloer)
            for dy in (0, 1, 2):
                S.set(*p(d, s, dy), "minecraft:air")
            S.set(*p(d, s, 3), plafond)
    S.set(*p(TUNNEL + 2, 0, 3), lamp)
    # the signs on the tunnel's end wall (the frame next to the tunnel mouth), facing the chamber
    S.set(*p(TUNNEL + 1, 1, 1), f"minecraft:{hout}_wall_sign", {"facing": richting, "waterlogged": "false"},
          bord_nbt(ms, [("~ Ingang ~", "~ Entrance ~"), naam, ("Kom binnen!", "Come on in!"), wie], texts, f"{prefix}_ingang"))
    S.set(*p(TUNNEL + 1, -1, 1), f"minecraft:{hout}_wall_sign", {"facing": richting, "waterlogged": "false"},
          bord_nbt(ms, [("Door de deur,", "Through the door"), ("dan rechtdoor!", "and straight on!"), ("Graven mag hier", "You may dig"),
                        ("gewoon, hoor.", "around here.")], texts, f"{prefix}_ingang2"))
    return p(TUNNEL + 2, 0, 0)


# =====================================================================================================================
# the hut on its foundation, the shaft
# =====================================================================================================================
def hut(ms, S, vloer_y=0, fundering="minecraft:cobblestone"):
    """The hut S (the block you stand on at y vloer_y) on foundation layers, so its floor is at y FUNDERING of the new
    template. The foundation fills the footprint of S's bottom layer; S's down jigsaw (in its bottom layer) moves to the
    bottom of the new template (the hut must not overlap the shaft)."""
    extra = FUNDERING - vloer_y
    assert extra >= 0
    sx, sy, sz = S.size
    T = verschuif(ms, S, 0, extra, 0, (sx, sy + extra, sz))
    onder = {(x, z) for (x, y, z) in S.blocks if y == 0 and S.blocks[(x, y, z)][0] != "minecraft:air"}
    for (x, z) in onder:
        for y in range(extra):
            T.set(x, y, z, fundering)
    for (x, y, z), b in list(T.blocks.items()):
        if b[0] == "minecraft:jigsaw" and y == extra and extra:
            onder_blok = b[2]["final_state"]
            T.blocks[(x, 0, z)] = b
            T.set(x, y, z, onder_blok)
    return T


def jigsaw(S, x, y, z, richting, naam, doel, pool, final):
    S.set(x, y, z, "minecraft:jigsaw", {"orientation": f"{richting}_north"},
          {"id": "minecraft:jigsaw", "name": naam, "target": doel, "pool": pool, "joint": "rollable",
           "final_state": final, "selection_priority": 0, "placement_priority": 0})


def schacht(ms, naam, sid, hoogte, final="minecraft:air"):
    """A 1 x hoogte x 1 column of nothing: the jigsaw down (onto the hall's lift jigsaw) and up (to the hut). Its two
    jigsaws become air (the hut may be moved up or down to the ground: "hut_op_grond"; a stone there could float)."""
    S = ms.Structure((1, hoogte, 1))
    jigsaw(S, 0, 0, 0, "down", f"guhs:{naam}_schacht_onder", "", "minecraft:empty", final)
    jigsaw(S, 0, hoogte - 1, 0, "up", f"guhs:{naam}_schacht_boven", f"guhs:{naam}_hut_onder", f"guhs:{sid}/hut", final)
    return S


def worldgen(h, sid, naam, *, templates, hal_hoogte, schacht_hoogte, biomes, salt, spacing=32, separation=11, keep_clear=40,
             check_radius=10, max_height_difference=8, step="underground_structures"):
    """The structure guhs:<sid> (start pool <sid>/start = the hall, <sid>/schacht, <sid>/hut), its set and biome tag.
    templates: {pool: template name}."""
    D_ = h.D
    h.w(f"{D_}/tags/worldgen/biome/has_structure/{sid}.json", {"values": biomes})
    h.w(f"{D_}/worldgen/structure_set/{sid}.json", {"structures": [{"structure": f"guhs:{sid}", "weight": 1}],
                                                     "placement": {"type": "minecraft:random_spread", "spacing": spacing,
                                                                   "separation": separation, "salt": salt}})
    for pool, loc in templates.items():
        el = {"element_type": "minecraft:single_pool_element", "location": f"guhs:{loc}", "projection": "rigid", "processors": "minecraft:empty"}
        if pool == "hut":   # (the floor you stand on is y FUNDERING: the first layer above the ground is FUNDERING + 1)
            el["element_type"] = "guhs:grond_single_pool_element"
            el["ground_level_delta"] = FUNDERING + 1
        h.w(f"{D_}/worldgen/template_pool/{sid}/{pool}.json", {"fallback": "minecraft:empty", "elements": [{"weight": 1, "element": el}]})
    geen = {"bounding_box": "piece", "spawns": []}
    jig = {"type": "minecraft:jigsaw", "biomes": f"#guhs:has_structure/{sid}", "step": step,
           "spawn_overrides": {"monster": geen, "ambient": geen}, "terrain_adaptation": "none",
           "start_pool": f"guhs:{sid}/start", "size": 3, "start_height": {"absolute": -(FUNDERING + schacht_hoogte + hal_hoogte)},
           "project_start_to_heightmap": "WORLD_SURFACE_WG", "max_distance_from_center": 80, "use_expansion_hack": False,
           "start_jigsaw_name": f"guhs:{naam}_lift"}
    h.w(f"{D_}/worldgen/structure/{sid}.json", {
        "type": "guhs:flat_jigsaw", "biomes": f"#guhs:has_structure/{sid}", "step": step,
        "spawn_overrides": {"monster": geen, "ambient": geen}, "terrain_adaptation": "none",
        "check_radius": check_radius, "max_height_difference": max_height_difference, "keep_clear": keep_clear, "hut_op_grond": True,
        "jigsaw": jig})


# =====================================================================================================================
# connected fences, panes and walls
# =====================================================================================================================
NIET_VOL = ("_slab", "_stairs", "_fence", "_fence_gate", "_wall", "_pane", "iron_bars", "_sign", "lantern", "torch", "_door",
            "_trapdoor", "ladder", "rail", "lever", "_button", "_pressure_plate", "_carpet", "chest", "_bed", "candle", "chain",
            "flower_pot", "potted_", "air", "water", "lava", "jigsaw", "structure_void", "leaves", "_banner", "_head", "_skull",
            "_plate", "_trap", "_muur", "hekje", "poortje", "glass_pane", "bars", "vine", "glow_lichen", "moss_carpet", "snow",
            "campfire", "bell", "anvil", "cauldron", "hopper", "_rod", "scaffolding", "cobweb", "flower", "grass", "fern",
            "mushroom", "sapling", "pumpkin", "melon")
_VOL = {}


def _model_vol(A, naam):
    """Is this guhs: block a full cube (its blockstate's first model has a cube parent)?"""
    pad = os.path.join(A, "blockstates", naam + ".json")
    if not os.path.exists(pad):
        return False
    bs = json.load(open(pad, encoding="utf-8"))
    if "variants" not in bs:
        return False
    v = next(iter(bs["variants"].values()))
    model = (v[0] if isinstance(v, list) else v)["model"]
    for _ in range(6):
        if ":" in model:
            ns, rest = model.split(":", 1)
        else:
            ns, rest = "minecraft", model
        if ns == "minecraft":
            return rest in ("block/cube_all", "block/cube_column", "block/cube", "block/cube_bottom_top", "block/cube_column_horizontal",
                            "block/orientable", "block/cube_directional", "block/cube_mirrored_all", "block/leaves") and "leaves" not in rest
        mp = os.path.join(A, "models", rest + ".json")
        if not os.path.exists(mp):
            return False
        m = json.load(open(mp, encoding="utf-8"))
        if "parent" not in m:
            return False
        model = m["parent"]
    return False


def vol(A, naam):
    """A block whose sides are full (fences, panes and walls attach to it)."""
    if naam is None:
        return False
    if naam not in _VOL:
        ns, kort = naam.split(":", 1)
        if ns == "guhs":
            _VOL[naam] = _model_vol(A, kort)
        else:
            _VOL[naam] = not any(t in kort for t in NIET_VOL)
    return _VOL[naam]


def soort(naam):
    """'hek', 'ruit', 'muur', 'poort' or None."""
    if naam is None:
        return None
    kort = naam.split(":", 1)[1]
    if kort.endswith("_fence_gate") or "poortje" in kort:
        return "poort"
    if kort.endswith("_fence") or "hekje" in kort:
        return "hek"
    if kort.endswith("_pane") or kort.endswith("iron_bars") or kort.endswith("_ruit"):
        return "ruit"
    if (kort.endswith("_wall") or kort.endswith("_muur")) and "sign" not in kort and "torch" not in kort and "banner" not in kort:
        return "muur"
    return None


def _hek_familie(naam):
    return "nether" if "nether_brick" in naam else "hout"


def verbind(S, A):
    """Gives every fence, pane, wall and iron bars in S the connection states its neighbours give it (like the game does
    when you place one). Returns how many changed."""
    n = 0
    for (x, y, z), (naam, props, nbt) in list(S.blocks.items()):
        s = soort(naam)
        if s not in ("hek", "ruit", "muur"):
            continue
        nieuw = dict(props)
        verbonden = {}
        for kant, (dx, dz) in D.items():
            buur = S.blocks.get((x + dx, y, z + dz))
            bnaam = buur[0] if buur else None
            bs = soort(bnaam)
            if bs == "poort":
                f = buur[1].get("facing", "north")
                ok = (f in ("north", "south")) != (kant in ("north", "south"))   # (a gate across the line)
            elif s == "hek":
                ok = bs == "hek" and _hek_familie(bnaam) == _hek_familie(naam) or vol(A, bnaam)
            else:
                ok = bs in ("ruit", "muur") or vol(A, bnaam)
            verbonden[kant] = ok
        if s == "muur":
            boven = S.blocks.get((x, y + 1, z))
            hoog = boven is not None and (vol(A, boven[0]) or soort(boven[0]) == "muur")
            for kant, ok in verbonden.items():
                nieuw[kant] = ("tall" if hoog else "low") if ok else "none"
            recht = (verbonden["north"] and verbonden["south"] and not verbonden["east"] and not verbonden["west"]) or \
                    (verbonden["east"] and verbonden["west"] and not verbonden["north"] and not verbonden["south"])
            nieuw["up"] = "false" if recht and not (boven and soort(boven[0]) == "muur") else "true"
        else:
            for kant, ok in verbonden.items():
                nieuw[kant] = "true" if ok else "false"
        nieuw["waterlogged"] = "false"
        if nieuw != props:
            S.blocks[(x, y, z)] = (naam, nieuw, nbt)
            n += 1
    return n


def check_verbonden(S, A, naam):
    """Problems: a fence, pane or wall whose state differs from what its neighbours give it."""
    kopie = type(S)(S.size)
    kopie.blocks = dict(S.blocks)
    verbind(kopie, A)
    return [f"{naam}: {kopie.blocks[p][0]} at {p} is not connected right" for p in S.blocks if kopie.blocks[p][1] != S.blocks[p][1]]


# =====================================================================================================================
# checks
# =====================================================================================================================
LOOP = ("minecraft:air", "minecraft:cave_air")


def _loopbaar(b):
    if b is None:
        return False
    naam = b[0]
    return naam in LOOP or naam.endswith("_door") or naam.endswith("_sign") or naam.endswith("lantern") or naam == "minecraft:ladder" \
        or naam.endswith("_carpet")


def doorgang(S, van, naar, binnen=None):
    """Can you get from `van` to `naar` (both spots you stand in) through air, doors and signs (2 high, or ladders)? Only
    through stored blocks (what's not in the template is the world's rock); `binnen`: an optional (x0, z0, x1, z1) limit."""
    van, naar = tuple(van), tuple(naar)
    gezien, rij = {van}, [van]
    while rij:
        p = rij.pop()
        if p == naar:
            return True
        x, y, z = p
        for q in ((x + 1, y, z), (x - 1, y, z), (x, y, z + 1), (x, y, z - 1), (x, y + 1, z), (x, y - 1, z)):
            if q in gezien or not _loopbaar(S.blocks.get(q)):
                continue
            if binnen and not (binnen[0] <= q[0] <= binnen[2] and binnen[1] <= q[2] <= binnen[3]):
                continue
            gezien.add(q)
            rij.append(q)
    return False


def check_gebouw(spel, zaal, info, hut, A):
    """The lifts (two free blocks above each, the hut's straight above the hall's jigsaw), the protected hall = the template
    minus RAND sideways (Java Spelhal.RAND), the extra entrances reachable from the guh, every guhs: block exists."""
    problems = []
    rand = re.search(r"\bRAND = (\d+)", open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "spelen", "Spelhal.java"),
                                              encoding="utf-8").read())
    if not rand or int(rand.group(1)) != RAND:
        problems.append(f"Spelhal.RAND != {RAND}")
    x0, y0, z0, x1, y1, z1 = info["hal"]
    sx, sy, sz = zaal.size
    if (x0, z0, sx - 1 - x1, sz - 1 - z1) != (RAND,) * 4 or (y0, y1) != (0, sy - 1):
        problems.append(f"{spel}: the hall is not the template minus RAND: {info['hal']} in {zaal.size}")
    lx, ly, lz = info["lift"]
    if zaal.get(lx, ly, lz) != "guhs:wolkenliftje" or any(zaal.get(lx, ly + d, lz) not in (None, "minecraft:air") for d in (1, 2)):
        problems.append(f"{spel}: the hall's Wolkenliftje has no room")
    jig = [p for p, b in zaal.blocks.items() if b[0] == "minecraft:jigsaw"]
    if jig != [(lx, sy - 1, lz)]:
        problems.append(f"{spel}: the lift jigsaw is not straight above the hall's Wolkenliftje: {jig}")
    hj = [p for p, b in hut.blocks.items() if b[0] == "minecraft:jigsaw"]
    hl = [p for p, b in hut.blocks.items() if b[0] == "guhs:wolkenliftje"]
    if len(hj) != 1 or len(hl) != 1 or hj[0][1] != 0 or (hj[0][0], hj[0][2]) != (hl[0][0], hl[0][2]) or hl[0][1] != FUNDERING:
        problems.append(f"{spel}: the hut's jigsaw {hj} / Wolkenliftje {hl}")
    elif any(hut.get(hl[0][0], hl[0][1] + d, hl[0][2]) not in (None, "minecraft:air") for d in (1, 2)):
        problems.append(f"{spel}: the hut's Wolkenliftje has no room")
    nx, ny, nz = info["npc"]
    for kamer in info["ingangen"]:
        if x0 <= kamer[0] <= x1 and z0 <= kamer[2] <= z1:
            problems.append(f"{spel}: the entrance chamber {kamer} is inside the protected hall")
        if not doorgang(zaal, (nx, ny, nz), kamer):
            problems.append(f"{spel}: no way from the guh to the entrance {kamer}")
    if len(info["ingangen"]) < 2:
        problems.append(f"{spel}: only {len(info['ingangen'])} extra entrances")
    have = {f[:-5] for f in os.listdir(os.path.join(A, "blockstates"))}
    for t in (zaal, hut):
        missing = sorted({b[0] for b in t.blocks.values() if b[0].startswith("guhs:") and b[0][5:] not in have})
        if missing:
            problems.append(f"{spel}: blocks without a blockstate: {missing}")
    return problems
