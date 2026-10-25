"""
Connected shapes in structure templates (shared by every template writer: make_structures.Structure.save calls verbind).

A jigsaw piece is placed with a "known shape" (no neighbour updates), so fences, walls, panes, bars and fence gates keep
exactly the state stored in the template. verbind() works their connections out from the template's own neighbours before
saving (like Minecraft's FenceBlock / WallBlock / IronBarsBlock / FenceGateBlock would). A neighbour that is not in the
template (nothing stored: structure_void, or outside the template) is the world's block or another piece: that side keeps
what was stored. The garlands (guhs:vlaggetjes, guhs:feestslingers, guhs:seizoensslinger: a horizontal axis) are laid along
their row.

Templates without a generator: tools/hekjes.py (scan / fix the .nbt files in place).
(Moved here from features/onderwol_bouw.py, which re-exports these names.)
"""
import json
import os

SCHIL = "minecraft:bedrock"   # (the onderwol buildings' rock marker: a full block)


def _blok(s):
    """'guhs:x[a=b,c=d]' -> ('guhs:x', {'a': 'b', 'c': 'd'})"""
    if "[" in s:
        naam, rest = s.split("[", 1)
        props = dict(kv.split("=") for kv in rest.rstrip("]").split(",") if kv)
    else:
        naam, props = s, {}
    if ":" not in naam:
        naam = "guhs:" + naam
    return naam, props


RES = os.path.join("src", "main", "resources")
RICHTINGEN = {"north": (0, 0, -1), "east": (1, 0, 0), "south": (0, 0, 1), "west": (-1, 0, 0)}
TEGEN = {"north": "south", "south": "north", "east": "west", "west": "east"}
_TAGS = {}
_VOL = {}
# (vanilla blocks that are not a full cube, by a part of their name; checked after VOL_TOCH)
NIET_VOL = ("air", "stairs", "slab", "fence", "_wall", "pane", "iron_bars", "door", "sign", "torch", "lantern", "carpet", "button", "lever",
            "ladder", "rail", "chest", "_bed", "candle", "flower", "tulip", "poppy", "dandelion", "orchid", "allium", "bluet", "daisy",
            "cornflower", "lily", "rose", "peony", "lilac", "grass", "fern", "sapling", "vine", "lichen", "pressure_plate", "banner", "head",
            "skull", "pot", "chain", "campfire", "bell", "lectern", "anvil", "cauldron", "hopper", "scaffolding", "snow", "cake", "leaves",
            "water", "lava", "cobweb", "jigsaw", "structure_void", "rod", "cluster", "_bud", "dripleaf", "azalea", "coral", "kelp",
            "seagrass", "sugar_cane", "bamboo", "mushroom", "brewing_stand", "enchanting_table", "grindstone", "stonecutter", "composter",
            "barrier", "pumpkin", "melon", "shulker", "farmland", "dirt_path", "conduit", "frame", "pickle", "egg", "trial", "vault",
            "spawner", "piston", "repeater", "comparator", "redstone_wire", "tripwire", "string", "web", "crop", "wheat", "carrots",
            "potatoes", "beetroots", "berry", "roots", "sprouts", "fungus", "nylium_plant", "plate")
VOL_TOCH = {"minecraft:grass_block", "minecraft:snow_block", "minecraft:bamboo_block", "minecraft:stripped_bamboo_block",
            "minecraft:bamboo_planks", "minecraft:bamboo_mosaic", "minecraft:mushroom_stem", "minecraft:red_mushroom_block",
            "minecraft:brown_mushroom_block", "minecraft:redstone_block", "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
            "minecraft:redstone_lamp", "minecraft:pumpkin", "minecraft:melon", "minecraft:carved_pumpkin", "minecraft:jack_o_lantern"}
# (full cubes that fences and panes still don't attach to: Block.isExceptionForConnection)
UITZONDERING = ("leaves", "barrier", "pumpkin", "melon", "shulker", "jack_o_lantern")
VOL_OUDERS = {"minecraft:block/cube", "minecraft:block/cube_all", "minecraft:block/cube_column", "minecraft:block/cube_column_horizontal",
              "minecraft:block/cube_bottom_top", "minecraft:block/cube_top", "minecraft:block/orientable", "minecraft:block/orientable_with_bottom",
              "minecraft:block/orientable_vertical", "minecraft:block/cube_directional", "minecraft:block/cube_mirrored_all",
              "minecraft:block/cube_north_west_mirrored_all", "minecraft:block/cube_column_mirrored", "minecraft:block/cube_mirrored",
              "minecraft:block/cube_all_inner_faces", "minecraft:block/leaves", "block/cube_all", "block/cube", "block/cube_column"}


def _tag(naam):
    """The values of a block tag minecraft:<naam> as this mod writes it (plus the vanilla blocks by their names)."""
    if naam not in _TAGS:
        p = os.path.join(RES, "data", "minecraft", "tags", "block", naam + ".json")
        _TAGS[naam] = set(json.load(open(p, encoding="utf-8"))["values"]) if os.path.exists(p) else set()
    return _TAGS[naam]


def soort(naam):
    """'fence', 'gate', 'wall', 'pane' or None for a block name."""
    if naam.startswith("minecraft:"):
        k = naam[10:]
        if k.endswith("_fence_gate"):
            return "gate"
        if k.endswith("_fence"):
            return "fence"
        if k.endswith("_wall") and not any(t in k for t in ("sign", "torch", "banner", "head", "skull", "fan")):
            return "wall"
        if k.endswith("_pane") or k == "iron_bars":
            return "pane"
        return None
    if naam in _tag("fence_gates"):
        return "gate"
    if naam in _tag("fences") or naam in _tag("wooden_fences"):
        return "fence"
    if naam in _tag("walls"):
        return "wall"
    if naam.endswith("_ruit") or naam == "guhs:roosterijzer_tralies":
        return "pane"
    return None


def _houten(naam):
    return naam in _tag("wooden_fences") or (naam.startswith("minecraft:") and naam.endswith("_fence") and naam != "minecraft:nether_brick_fence")


def _model_vol(model, diepte=0):
    if diepte > 8 or not model:
        return False
    if ":" not in model:
        model = "minecraft:" + model
    if model in VOL_OUDERS:
        return True
    ns, pad = model.split(":", 1)
    if ns != "guhs":
        return False
    p = os.path.join(RES, "assets", "guhs", "models", *pad.split("/")) + ".json"
    if not os.path.exists(p):
        return False
    m = json.load(open(p, encoding="utf-8"))
    if "elements" in m:
        return any(e.get("from") == [0, 0, 0] and e.get("to") == [16, 16, 16] for e in m["elements"])
    return _model_vol(m.get("parent"), diepte + 1)


def vol(naam):
    """Is this block a full cube (every face sturdy)? SCHIL is the world's rock: always."""
    if naam == SCHIL:
        return True
    if naam not in _VOL:
        if naam.startswith("minecraft:"):
            _VOL[naam] = naam in VOL_TOCH or not any(t in naam[10:] for t in NIET_VOL)
        else:
            p = os.path.join(RES, "assets", "guhs", "blockstates", naam.split(":", 1)[1] + ".json")
            ok = False
            if os.path.exists(p):
                bs = json.load(open(p, encoding="utf-8"))
                if "variants" in bs and bs["variants"]:
                    v = next(iter(bs["variants"].values()))
                    v = v[0] if isinstance(v, list) else v
                    ok = _model_vol(v.get("model"))
            _VOL[naam] = ok
    return _VOL[naam]


def _echt(blok):
    """A stored block as (name, props): a jigsaw counts as its final state."""
    if blok is None:
        return None
    naam, props, nbt = blok
    if naam == "minecraft:jigsaw" and nbt:
        return _blok(nbt.get("final_state", "minecraft:air"))
    return naam, props


def stevig(blok, kant):
    """Is the face 'kant' (north/east/south/west) of this (name, props) sturdy?"""
    naam, props = blok
    if any(t in naam for t in UITZONDERING):
        return False
    if "shape" in props and "half" in props and "facing" in props:      # stairs: their back is full
        return props["facing"] == kant
    if props.get("type") == "double":                                   # a double slab
        return True
    if props.get("type") in ("top", "bottom") or soort(naam):
        return False
    return vol(naam)


def _poort_dwars(blok, richting):
    """A fence gate attaches to a fence/wall on its sides: its facing axis across the direction towards it."""
    f = blok[1].get("facing", "north")
    return (f in ("north", "south")) != (richting in ("north", "south"))


def verbind(b):
    """verbind_blokken on a make_structures.Structure (its .blocks)."""
    verbind_blokken(b.blocks)


def verbind_blokken(blocks):
    """Work out the connections of every fence, wall, pane, bar and fence gate from the template's neighbours (a neighbour
    that is not in the template, the world's own block, leaves the side as it was), and lay the garlands (vlaggetjes,
    feestslingers, seizoensslingers) along their row. blocks: {(x, y, z): (name, props, nbt)}, changed in place."""
    def buur(p, r):
        d = RICHTINGEN[r]
        return _echt(blocks.get((p[0] + d[0], p[1] + d[1], p[2] + d[2])))
    verbonden = {}
    for p, (naam, props, nbt) in list(blocks.items()):
        s = soort(naam)
        if s is None:
            continue
        nieuw = dict(props)
        if s == "gate":
            zij = ("east", "west") if props.get("facing", "north") in ("north", "south") else ("north", "south")
            nieuw["in_wall"] = "true" if any((n := buur(p, r)) and soort(n[0]) == "wall" for r in zij) else "false"
            blocks[p] = (naam, nieuw, nbt)
            continue
        kanten = {}
        for r in RICHTINGEN:
            n = buur(p, r)
            if n is None:
                continue
            ns = soort(n[0])
            if n[0] in ("minecraft:air", "minecraft:cave_air", "minecraft:void_air"):
                c = False
            elif s == "fence":
                c = (ns == "fence" and _houten(n[0]) == _houten(naam)) or (ns == "gate" and _poort_dwars(n, r)) or stevig(n, TEGEN[r])
            elif s == "pane":
                c = ns in ("pane", "wall") or stevig(n, TEGEN[r])
            else:
                c = ns in ("wall", "pane") or (ns == "gate" and _poort_dwars(n, r)) or stevig(n, TEGEN[r])
            kanten[r] = c
        verbonden[p] = (s, kanten)
        if s in ("fence", "pane"):
            for r, c in kanten.items():
                nieuw[r] = "true" if c else "false"
            nieuw.setdefault("waterlogged", "false")
            blocks[p] = (naam, nieuw, nbt)
    # the walls: low or tall per side (tall under a full block or a wall that goes the same way), and the post
    for p, (s, kanten) in sorted(verbonden.items(), key=lambda t: -t[0][1]):   # (top down: a post looks at the wall above)
        if s != "wall":
            continue
        naam, props, nbt = blocks[p]
        nieuw = dict(props)
        boven_p = (p[0], p[1] + 1, p[2])
        boven = _echt(blocks.get(boven_p))
        # (WallBlock.isCovered: the bottom face of the block above)
        onderkant_vol = boven is not None and soort(boven[0]) is None and (
            boven[1].get("type") in ("bottom", "double") or ("shape" in boven[1] and boven[1].get("half") == "bottom")
            or (boven[1].get("type") is None and "shape" not in boven[1] and vol(boven[0])))
        for r in RICHTINGEN:
            c = kanten[r] if r in kanten else props.get(r, "none") != "none"
            if not c:
                nieuw[r] = "none"
                continue
            hoog = onderkant_vol or (boven is not None and soort(boven[0]) == "wall" and verbonden.get(boven_p, (None, {}))[1].get(r, False))
            nieuw[r] = "tall" if hoog else "low"
        n_, e_, s_, w_ = (nieuw[r] == "none" for r in ("north", "east", "south", "west"))
        # (WallBlock.shouldRaisePost)
        if boven is not None and soort(boven[0]) == "wall" and boven[1].get("up") == "true":
            post = True
        elif (n_ and s_ and e_ and w_) or n_ != s_ or e_ != w_:
            post = True
        elif (nieuw["north"] == "tall" and nieuw["south"] == "tall") or (nieuw["east"] == "tall" and nieuw["west"] == "tall"):
            post = False
        else:
            post = onderkant_vol or (boven is not None and any(t in boven[0] for t in ("torch", "lantern", "sign", "banner", "pressure_plate",
                                                                                      "lantaarn", "lampion", "lampje")))
        nieuw["up"] = "true" if post else "false"
        nieuw.setdefault("waterlogged", "false")
        blocks[p] = (naam, nieuw, nbt)
    slingers(blocks)


# =====================================================================================================================
# garlands: a string of flags along one horizontal axis. A row of them (garland neighbours in the template) follows the
# row; a single one hangs between what is on its sides (supports on both sides of the one axis, not of the other). Else
# it stays as stored.
# =====================================================================================================================
SLINGERS = ("guhs:vlaggetjes", "guhs:feestslingers", "guhs:seizoensslinger")


def _drager(blok):
    return blok is not None and blok[0] not in ("minecraft:air", "minecraft:cave_air") and (
        soort(blok[0]) is not None or vol(blok[0]) or "paal" in blok[0] or "balk" in blok[0])


def slingers(blocks):
    for p, (naam, props, nbt) in list(blocks.items()):
        if naam not in SLINGERS or "axis" not in props:
            continue
        rond = {r: _echt(blocks.get((p[0] + d[0], p[1] + d[1], p[2] + d[2]))) for r, d in RICHTINGEN.items()}
        rij = {a: sum(1 for r in rs if rond[r] is not None and rond[r][0] in SLINGERS)
               for a, rs in (("x", ("east", "west")), ("z", ("north", "south")))}
        if rij["x"] and not rij["z"]:
            as_ = "x"
        elif rij["z"] and not rij["x"]:
            as_ = "z"
        elif not rij["x"] and not rij["z"]:
            dx = _drager(rond["east"]) and _drager(rond["west"])
            dz = _drager(rond["north"]) and _drager(rond["south"])
            as_ = "x" if dx and not dz else "z" if dz and not dx else props["axis"]
        else:
            as_ = props["axis"]
        if as_ != props["axis"]:
            blocks[p] = (naam, {**props, "axis": as_}, nbt)
