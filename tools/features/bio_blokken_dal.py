"""
biomes3 slice "blokken-dal" (Java: feature/bio/blokkendal; English: tools/lang/en/c82_bio_blokken_dal.json; contract: CONTRACT_BIO.md).

The Klaterdal block sets: roze lakhout, guh-dakpannen (with the krul that turns an eave up), shoji and tatami, the garden
things (toro, geharkt zand, guh-bamboe, bonsai in a pot, gladde knuffelsteen) and the esdoorn. Textures:
bio_blokken_dal_tex.py. Everything here is a block with an item, a model, a loot table, a recipe, tags and a Dutch name.

What a player does in the Klaterdal, in order (the recipes follow it):
  chop an esdoorn           -> roze lakhoutplanken (4 a trunk), and from those the slab, stairs, fence, gate, door, trapdoor, balk
  (no esdoorn near)         -> 8 planks of any wood + roze verf -> 8 roze lakhoutplanken
  dig gladde knuffelsteen   -> slab and stairs; four -> witte guh-dakpannen; with a torch -> the toro; with a sapling -> a bonsai
  (far from a valley)       -> knuffelsteen in a furnace -> gladde knuffelsteen
  dye the dakpannen         -> roze and zachtgrijs; from each colour the slab, the stairs and the krul
  cut guh-bamboe            -> paper, sticks, tatami; with paper -> shoji
  rake sand with a hoe      -> geharkt zand (or craft it from two sand); the hoe again -> the next pattern
"""
import json
import os
import zipfile

from features import bio_blokken_dal_tex as tex
from features import bio_lib as lib

KLEUREN = {"roze": ("Roze", "minecraft:pink_dye"), "wit": ("Witte", "minecraft:white_dye"), "grijs": ("Zachtgrijze", "minecraft:light_gray_dye")}
HOUT = {"roze_lakhout_trap": "oak_stairs", "roze_lakhout_plaat": "oak_slab", "roze_lakhout_hek": "oak_fence",
        "roze_lakhout_poort": "oak_fence_gate", "roze_lakhout_deur": "oak_door", "roze_lakhout_luik": "oak_trapdoor"}
# the curl of the krul in eight bands of two pixels, roof side first: top and underside (DakpanHoekBlock has the same numbers)
KRUL_BOVEN = [8, 8, 9, 10, 12, 14, 16, 19]
KRUL_ONDER = [0, 0, 2, 4, 6, 8, 10, 13]
BONSAI_VORMEN = 4

NAMEN = {
    "roze_lakhout_planken": "Roze lakhoutplanken", "roze_lakhout_plaat": "Roze lakhoutplaat", "roze_lakhout_trap": "Roze lakhouttrap",
    "roze_lakhout_hek": "Roze lakhouthek", "roze_lakhout_poort": "Roze lakhoutpoort", "roze_lakhout_deur": "Roze lakhoutdeur",
    "roze_lakhout_luik": "Roze lakhoutluik", "roze_lakhout_balk": "Roze lakhoutbalk",
    "shoji": "Shoji-schuifpaneel", "tatami": "Tatamimat", "toro": "Stenen guh-lantaarn",
    "geharkt_zand": "Geharkt zand", "geharkt_zand_ring": "Geharkt zand met ringen",
    "guh_bamboe": "Guh-bamboe", "bonsai_pot": "Bonsai in een potje",
    "gladde_knuffelsteen": "Gladde knuffelsteen", "gladde_knuffelsteen_plaat": "Gladde knuffelsteenplaat",
    "gladde_knuffelsteen_trap": "Gladde knuffelsteentrap",
    "esdoorn_stam": "Esdoornstam", "esdoorn_bladeren_rood": "Rode esdoornbladeren", "esdoorn_bladeren_oranje": "Oranje esdoornbladeren",
    "esdoorn_zaailing": "Esdoornzaailing",
}
for _k, (_naam, _verf) in KLEUREN.items():
    NAMEN[f"guh_dakpan_{_k}"] = f"{_naam} guh-dakpannen"
    NAMEN[f"guh_dakpan_{_k}_plaat"] = f"{_naam} guh-dakpanplaat"
    NAMEN[f"guh_dakpan_{_k}_trap"] = f"{_naam} guh-dakpantrap"
    NAMEN[f"guh_dakpan_{_k}_hoek"] = f"{_naam} guh-dakkrul"
UITLEG = {  # the grey line under the item's name (BlokkenDalSlice.MET_UITLEG)
    "shoji": "Klik: het paneel schuift open. Twee naast elkaar schuiven samen open. Njeg!",
    "tatami": "Twee naast elkaar worden één mat. Sluipen: een halve mat. Lekker vads liggen.",
    "toro": "Gaat vanzelf aan als het donker wordt, en 's ochtends weer uit.",
    "geharkt_zand": "Hark met een schoffel voor een ander patroon. Gewoon zand kun je ook harken.",
    "geharkt_zand_ring": "Legt vanzelf ringen om een kei of een rondje. Sluipen met een schoffel: zelf draaien.",
    "bonsai_pot": "Knip met een schaar voor een andere vorm. Knip knip, njeg.",
}
for _k in KLEUREN:
    UITLEG[f"guh_dakpan_{_k}_hoek"] = "Krult vanzelf omhoog aan het eind en op de hoek van je dakrand. Vahoeg!"
BLOCKS = list(NAMEN)


def _jar():
    return zipfile.ZipFile(os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar"))


def copy_set(h, z, names, mapping, textures, group, base_ours, base_vanilla):
    """Vanilla's blockstates, models, loot and recipes of `mapping` (ours -> vanilla's), with our textures and items."""
    A, D, w = h.A, h.D, h.w
    for ours, van in mapping.items():
        state = json.dumps(json.loads(z.read(f"assets/minecraft/blockstates/{van}.json")))
        for old, new in textures.items():
            state = state.replace(f'"{old}"', f'"{new}"')
        state = state.replace(f"minecraft:block/{van}", f"guhs:block/{ours}")
        w(f"{A}/blockstates/{ours}.json", json.loads(state))
        for path in names:
            if not path.startswith(f"assets/minecraft/models/block/{van}") or not path.endswith(".json"):
                continue
            rest = path[len(f"assets/minecraft/models/block/{van}"):-5]
            if van == "oak_fence" and rest.startswith("_gate"):
                continue
            model = json.loads(z.read(path))
            model["textures"] = {k: textures.get(v, v) for k, v in model.get("textures", {}).items()}
            if van in ("oak_door", "oak_trapdoor"):
                model["render_type"] = "minecraft:cutout"
            w(f"{A}/models/block/{ours}{rest}.json", model)
        loot = json.loads(z.read(f"data/minecraft/loot_table/blocks/{van}.json"))
        w(f"{D}/loot_table/blocks/{ours}.json", json.loads(json.dumps(loot).replace(f"minecraft:{van}", f"guhs:{ours}")))
        recipe = json.dumps(json.loads(z.read(f"data/minecraft/recipe/{van}.json")))
        recipe = recipe.replace(f'"item": "minecraft:{base_vanilla}"', f'"item": "guhs:{base_ours}"')
        recipe = recipe.replace(f'"id": "minecraft:{van}"', f'"id": "guhs:{ours}"').replace('"group": "wooden_', f'"group": "{group}_')
        w(f"{D}/recipe/{ours}.json", json.loads(recipe))


def _vlak(tex_ref, uv, **extra):
    return {"texture": tex_ref, "uv": [round(v, 3) for v in uv], **extra}


def doos(x0, y0, z0, x1, y1, z1, t="#all", faces=("down", "up", "north", "south", "west", "east")):
    """A box with UVs that follow its place in the block (and stay inside the texture when it sticks out above the block)."""
    hoogte = y1 - y0
    v0 = max(0, 16 - y1)
    v0 = min(v0, 16 - hoogte)
    f = {"up": _vlak(t, (x0 % 16, z0 % 16, x0 % 16 + (x1 - x0), z0 % 16 + (z1 - z0))),
         "down": _vlak(t, (x0 % 16, z0 % 16, x0 % 16 + (x1 - x0), z0 % 16 + (z1 - z0))),
         "north": _vlak(t, (x0 % 16, v0, x0 % 16 + (x1 - x0), v0 + hoogte)),
         "south": _vlak(t, (x0 % 16, v0, x0 % 16 + (x1 - x0), v0 + hoogte)),
         "west": _vlak(t, (z0 % 16, v0, z0 % 16 + (z1 - z0), v0 + hoogte)),
         "east": _vlak(t, (z0 % 16, v0, z0 % 16 + (z1 - z0), v0 + hoogte))}
    return {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": {k: f[k] for k in faces}}


def _draai(naam, extra=None):
    """Blockstate variants for the four facings of one model (made for north)."""
    return {f: {"model": f"guhs:block/{naam}", **({"y": y} if y else {}), **(extra or {})}
            for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}


# =====================================================================================================================
# roze lakhout
# =====================================================================================================================
def lakhout(h, z, names):
    A, w = h.A, h.w
    h.simple_block("roze_lakhout_planken")
    t = {"minecraft:block/oak_planks": "guhs:block/roze_lakhout_planken", "minecraft:block/oak_door_top": "guhs:block/roze_lakhout_deur_top",
         "minecraft:block/oak_door_bottom": "guhs:block/roze_lakhout_deur_bottom", "minecraft:block/oak_trapdoor": "guhs:block/roze_lakhout_luik"}
    copy_set(h, z, names, HOUT, t, "roze_lakhout", "roze_lakhout_planken", "oak_planks")
    w(f"{A}/models/item/roze_lakhout_trap.json", {"parent": "guhs:block/roze_lakhout_trap"})
    w(f"{A}/models/item/roze_lakhout_plaat.json", {"parent": "guhs:block/roze_lakhout_plaat"})
    w(f"{A}/models/item/roze_lakhout_hek.json", {"parent": "guhs:block/roze_lakhout_hek_inventory"})
    w(f"{A}/models/item/roze_lakhout_poort.json", {"parent": "guhs:block/roze_lakhout_poort"})
    w(f"{A}/models/item/roze_lakhout_luik.json", {"parent": "guhs:block/roze_lakhout_luik_bottom"})
    h.item_model("roze_lakhout_deur")
    # the balk: a pillar
    t = {"end": "guhs:block/roze_lakhout_balk_top", "side": "guhs:block/roze_lakhout_balk"}
    w(f"{A}/models/block/roze_lakhout_balk.json", {"parent": "minecraft:block/cube_column", "textures": t})
    w(f"{A}/models/block/roze_lakhout_balk_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": t})
    w(f"{A}/blockstates/roze_lakhout_balk.json", {"variants": {
        "axis=y": {"model": "guhs:block/roze_lakhout_balk"},
        "axis=z": {"model": "guhs:block/roze_lakhout_balk_horizontal", "x": 90},
        "axis=x": {"model": "guhs:block/roze_lakhout_balk_horizontal", "x": 90, "y": 90}}})
    w(f"{A}/models/item/roze_lakhout_balk.json", {"parent": "guhs:block/roze_lakhout_balk"})
    for b in ("roze_lakhout_planken", "roze_lakhout_balk"):
        h.self_drop(b)
    # recipes: from an esdoorn trunk, or any planks with roze verf; the balk from three planks above each other
    h.shapeless("roze_lakhout_planken", ["guhs:esdoorn_stam"], "guhs:roze_lakhout_planken", 4)
    h.shaped("roze_lakhout_planken_lakken", ["PPP", "PVP", "PPP"], {"P": "#minecraft:planks", "V": "minecraft:pink_dye"}, "guhs:roze_lakhout_planken", 8)
    h.shaped("roze_lakhout_balk", ["P", "P", "P"], {"P": "guhs:roze_lakhout_planken"}, "guhs:roze_lakhout_balk", 3)
    add = h.add_tag
    for kind in ("block", "item"):
        add(f"minecraft/tags/{kind}/planks", ["guhs:roze_lakhout_planken"])
        add(f"minecraft/tags/{kind}/wooden_stairs", ["guhs:roze_lakhout_trap"])
        add(f"minecraft/tags/{kind}/wooden_slabs", ["guhs:roze_lakhout_plaat"])
        add(f"minecraft/tags/{kind}/wooden_fences", ["guhs:roze_lakhout_hek"])
        add(f"minecraft/tags/{kind}/fence_gates", ["guhs:roze_lakhout_poort"])
        add(f"minecraft/tags/{kind}/wooden_doors", ["guhs:roze_lakhout_deur"])
        add(f"minecraft/tags/{kind}/wooden_trapdoors", ["guhs:roze_lakhout_luik"])
    add("minecraft/tags/block/mineable/axe", [f"guhs:{b}" for b in ["roze_lakhout_planken", *HOUT, "roze_lakhout_balk"]])


# =====================================================================================================================
# guh-dakpannen
# =====================================================================================================================
def krul_model(vorm):
    """The krul's boxes, made for a tip to the north (the roof is south). `hoek` and `eind` are the corner of north and west."""
    el = []
    for k in range(8):
        ver, dicht = 16 - 2 * k, 14 - 2 * k          # this band: from `ver` (roof side) to `dicht` (tip side) pixels
        onder, boven = KRUL_ONDER[k], KRUL_BOVEN[k]
        if vorm == "recht":
            el.append(doos(0, onder, dicht, 16, boven, ver))
        elif vorm == "hoek":                           # low against the east and the south, up to the north-west corner
            el.append(doos(0, onder, dicht, ver, boven, ver))
            if dicht > 0:
                el.append(doos(dicht, onder, 0, ver, boven, dicht))
        else:                                          # "eind": high along the north and the west
            el.append(doos(dicht, onder, dicht, 16, boven, ver))
            if ver < 16:
                el.append(doos(dicht, onder, ver, ver, boven, 16))
    if vorm == "eind":                                 # the lip runs all along here, so the corner itself gets a horn on top
        el += [doos(0, 16, 0, 6, 21, 6), doos(0, 20, 0, 4, 24, 4), doos(0, 23, 0, 2, 27, 2)]
    return el


def dakpannen(h, z, names):
    A, D, w = h.A, h.D, h.w
    add = h.add_tag
    for kleur, (_naam, verf) in KLEUREN.items():
        b = f"guh_dakpan_{kleur}"
        h.simple_block(b)
        h.self_drop(b)
        copy_set(h, z, names, {f"{b}_trap": "brick_stairs", f"{b}_plaat": "brick_slab"}, {"minecraft:block/bricks": f"guhs:block/{b}"},
                 b, b, "bricks")
        w(f"{A}/models/item/{b}_trap.json", {"parent": f"guhs:block/{b}_trap"})
        w(f"{A}/models/item/{b}_plaat.json", {"parent": f"guhs:block/{b}_plaat"})
        # the krul: three shapes, turned by the blockstate
        for vorm in ("recht", "hoek", "eind"):
            w(f"{A}/models/block/{b}_hoek_{vorm}.json", {"parent": "minecraft:block/block", "textures": {
                "all": f"guhs:block/{b}", "particle": f"guhs:block/{b}"}, "elements": krul_model(vorm)})
        variants = {}
        for i, f in enumerate(("north", "east", "south", "west")):
            def v(model, kwart):
                y = (i + kwart) % 4 * 90
                return {"model": f"guhs:block/{b}_hoek_{model}", **({"y": y} if y else {})}
            variants[f"facing={f},vorm=recht"] = v("recht", 0)
            variants[f"facing={f},vorm=hoek_links"] = v("hoek", 0)
            variants[f"facing={f},vorm=hoek_rechts"] = v("hoek", 1)
            variants[f"facing={f},vorm=eind_links"] = v("eind", 0)
            variants[f"facing={f},vorm=eind_rechts"] = v("eind", 1)
        w(f"{A}/blockstates/{b}_hoek.json", {"variants": variants})
        w(f"{A}/models/item/{b}_hoek.json", {"parent": f"guhs:block/{b}_hoek_hoek"})
        h.self_drop(f"{b}_hoek")
        # recipes: the colour (8 dakpannen of any colour around the dye), the krul (three in a corner), the stonecutter
        h.shaped(f"{b}_verven", ["PPP", "PVP", "PPP"], {"P": "#guhs:guh_dakpan", "V": verf}, f"guhs:{b}", 8)
        h.shaped(f"{b}_hoek", ["P ", "PP"], {"P": f"guhs:{b}"}, f"guhs:{b}_hoek", 3)
        for result, n in ((f"{b}_trap", 1), (f"{b}_plaat", 2), (f"{b}_hoek", 1)):
            w(f"{D}/recipe/{result}_steenzagen.json", {"type": "minecraft:stonecutting", "ingredient": {"item": f"guhs:{b}"},
                                                       "result": {"id": f"guhs:{result}", "count": n}})
        for kind in ("block", "item"):
            add(f"guhs/tags/{kind}/guh_dakpan", [f"guhs:{b}"])
            add(f"minecraft/tags/{kind}/stairs", [f"guhs:{b}_trap"])
            add(f"minecraft/tags/{kind}/slabs", [f"guhs:{b}_plaat"])
        add("minecraft/tags/block/mineable/pickaxe", [f"guhs:{b}", f"guhs:{b}_trap", f"guhs:{b}_plaat", f"guhs:{b}_hoek"])
    # the first dakpannen: four gladde knuffelsteen (or one in the stonecutter)
    h.shaped("guh_dakpan_wit", ["SS", "SS"], {"S": "guhs:gladde_knuffelsteen"}, "guhs:guh_dakpan_wit", 4)
    w(f"{D}/recipe/guh_dakpan_wit_steenzagen.json", {"type": "minecraft:stonecutting", "ingredient": {"item": "guhs:gladde_knuffelsteen"},
                                                     "result": {"id": "guhs:guh_dakpan_wit", "count": 1}})


# =====================================================================================================================
# shoji and tatami
# =====================================================================================================================
def shoji_tatami(h):
    A, w = h.A, h.w

    def paneel(x0, z0, x1, z1):
        """The panel seen from the south (FACING north: the placer looked north), between x0..x1 and z0..z1."""
        rand = "#rand"
        return {"from": [x0, 0, z0], "to": [x1, 16, z1], "faces": {
            "north": _vlak("#paneel", (16, 0, 0, 16)), "south": _vlak("#paneel", (0, 0, 16, 16)),
            "west": _vlak(rand, (0, 0, 2, 16)), "east": _vlak(rand, (2, 0, 4, 16)),
            "up": _vlak(rand, (0, 0, 16, 2)), "down": _vlak(rand, (0, 2, 16, 4))}}
    t = {"paneel": "guhs:block/shoji", "rand": "guhs:block/shoji_rand", "particle": "guhs:block/shoji"}
    # closed: in the middle of the block. Open: slid 13 pixels to the left or right, one layer further from the placer, so
    # it stands behind the next panel (or inside the wall); three pixels of it stay in sight against the post
    for naam, el in (("shoji", paneel(0, 7, 16, 9)), ("shoji_open_links", paneel(-13, 5, 3, 7)), ("shoji_open_rechts", paneel(13, 5, 29, 7))):
        w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/block", "textures": t, "elements": [el]})
    variants = {}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for kant, naam in (("left", "links"), ("right", "rechts")):
            r = {"y": y} if y else {}
            variants[f"facing={f},hinge={kant},open=false"] = {"model": "guhs:block/shoji", **r}
            variants[f"facing={f},hinge={kant},open=true"] = {"model": f"guhs:block/shoji_open_{naam}", **r}
    w(f"{A}/blockstates/shoji.json", {"variants": variants})
    h.item_model("shoji", "guhs:block/shoji")
    h.self_drop("shoji")
    # tatami: the top turns with FACING (the image's top is the mat's far end)
    for naam in ("los", "kop"):
        w(f"{A}/models/block/tatami_{naam}.json", {"parent": "minecraft:block/cube", "textures": {
            "up": f"guhs:block/tatami_{naam}", "down": "guhs:block/tatami_onder", "north": "guhs:block/tatami_zij_kop",
            "south": "guhs:block/tatami_zij_kop", "west": "guhs:block/tatami_zij_rand", "east": "guhs:block/tatami_zij_rand",
            "particle": f"guhs:block/tatami_{naam}"}})
    variants = {}
    for f, v in _draai("tatami_los").items():
        variants[f"facing={f},gekoppeld=false"] = v
    for f, v in _draai("tatami_kop").items():
        variants[f"facing={f},gekoppeld=true"] = v
    w(f"{A}/blockstates/tatami.json", {"variants": variants})
    w(f"{A}/models/item/tatami.json", {"parent": "guhs:block/tatami_los"})
    h.self_drop("tatami")
    # recipes: all from guh-bamboe
    h.shaped("guh_bamboe_papier", ["BBB"], {"B": "guhs:guh_bamboe"}, "minecraft:paper", 3)
    h.shaped("guh_bamboe_stok", ["B", "B"], {"B": "guhs:guh_bamboe"}, "minecraft:stick", 1)
    h.shaped("shoji", ["BPB", "BPB", "BPB"], {"B": "guhs:guh_bamboe", "P": "minecraft:paper"}, "guhs:shoji", 4)
    h.shaped("tatami", ["BBB", "BBB"], {"B": "guhs:guh_bamboe"}, "guhs:tatami", 3)
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:shoji", "guhs:tatami"])


# =====================================================================================================================
# tuinspul: toro, geharkt zand, guh-bamboe, bonsai, gladde knuffelsteen
# =====================================================================================================================
def toro(h):
    A, w = h.A, h.w
    steen = [doos(3, 0, 3, 13, 2, 13), doos(5, 2, 5, 11, 3, 11), doos(6, 3, 6, 10, 7, 10), doos(4, 7, 4, 12, 8, 12),
             doos(5, 8, 5, 11, 12, 11), doos(2, 12, 2, 14, 13, 14), doos(3, 13, 3, 13, 14, 13), doos(7, 14, 7, 9, 15, 9),
             doos(3, 14, 7, 5, 16, 9), doos(11, 14, 7, 13, 16, 9)]        # (the last two: the guh ears on the cap)
    gezicht = ((6, 10, 7, 11), (9, 10, 10, 11), (7, 9, 9, 10))             # two eyes and a mouth in each side of the lamp box

    def venster(aan):
        el = []
        for (u0, v0, u1, v1) in gezicht:
            for kant, frm, to in (("north", [16 - u1, v0, 4.98], [16 - u0, v1, 5]), ("south", [u0, v0, 11], [u1, v1, 11.02]),
                                  ("west", [4.98, v0, u0], [5, v1, u1]), ("east", [11, v0, 16 - u1], [11.02, v1, 16 - u0])):
                e = {"from": frm, "to": to, "shade": False, "faces": {kant: {"texture": "#venster", "uv": [u0, 16 - v1, u1, 16 - v0]}}}
                if aan:
                    e["light_emission"] = 15
                el.append(e)
        return el
    for naam, aan in (("toro", False), ("toro_aan", True)):
        w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/block", "textures": {
            "all": "guhs:block/toro", "particle": "guhs:block/toro", "venster": "guhs:block/toro_licht" if aan else "guhs:block/toro_donker"},
            "elements": steen + venster(aan)})
    w(f"{A}/blockstates/toro.json", {"variants": {"lit=false": {"model": "guhs:block/toro"}, "lit=true": {"model": "guhs:block/toro_aan"}}})
    w(f"{A}/models/item/toro.json", {"parent": "guhs:block/toro"})
    h.self_drop("toro")
    h.shaped("toro", ["P", "T", "S"], {"P": "guhs:gladde_knuffelsteen_plaat", "T": "minecraft:torch", "S": "guhs:gladde_knuffelsteen"}, "guhs:toro", 1)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:toro"])


def zand(h):
    A, w = h.A, h.w

    def kubus(naam, boven):
        z = "guhs:block/geharkt_zand_zij"
        w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/cube", "textures": {
            "up": f"guhs:block/{boven}", "down": z, "north": z, "south": z, "west": z, "east": z, "particle": f"guhs:block/{boven}"}})
    kubus("geharkt_zand", "geharkt_zand")
    w(f"{A}/blockstates/geharkt_zand.json", {"variants": {"axis=x": {"model": "guhs:block/geharkt_zand"},
                                                           "axis=z": {"model": "guhs:block/geharkt_zand", "y": 90}}})
    w(f"{A}/models/item/geharkt_zand.json", {"parent": "guhs:block/geharkt_zand"})
    for deel in ("rond", "rand", "hoek"):
        kubus(f"geharkt_zand_ring_{deel}", f"geharkt_zand_ring_{deel}")
    variants = {"vorm=rond": {"model": "guhs:block/geharkt_zand_ring_rond"}}
    # (the textures are made for the piece north of the centre and the piece north-east of it)
    for vorm, y in (("noord", 0), ("oost", 90), ("zuid", 180), ("west", 270)):
        variants[f"vorm={vorm}"] = {"model": "guhs:block/geharkt_zand_ring_rand", **({"y": y} if y else {})}
    for vorm, y in (("noordoost", 0), ("zuidoost", 90), ("zuidwest", 180), ("noordwest", 270)):
        variants[f"vorm={vorm}"] = {"model": "guhs:block/geharkt_zand_ring_hoek", **({"y": y} if y else {})}
    w(f"{A}/blockstates/geharkt_zand_ring.json", {"variants": variants})
    w(f"{A}/models/item/geharkt_zand_ring.json", {"parent": "guhs:block/geharkt_zand_ring_rond"})
    for b in ("geharkt_zand", "geharkt_zand_ring"):
        h.self_drop(b)
    h.shaped("geharkt_zand", ["ZZ"], {"Z": "minecraft:sand"}, "guhs:geharkt_zand", 2)
    h.shapeless("geharkt_zand_ring", ["guhs:geharkt_zand"], "guhs:geharkt_zand_ring", 1)
    h.shapeless("geharkt_zand_uit_ring", ["guhs:geharkt_zand_ring"], "guhs:geharkt_zand", 1)
    h.add_tag("minecraft/tags/block/mineable/shovel", ["guhs:geharkt_zand", "guhs:geharkt_zand_ring"])
    h.add_tag("guhs/tags/block/geharkt_zand_harkbaar", ["minecraft:sand"])


def bamboe(h):
    A, w = h.A, h.w
    stengel = {"from": [6.5, 0, 6.5], "to": [9.5, 16, 9.5], "faces": {
        **{f: {"texture": "#stengel", "uv": [0, 0, 3, 16]} for f in ("north", "south", "west", "east")},
        "up": {"texture": "#stengel", "uv": [0, 5, 3, 8]}, "down": {"texture": "#stengel", "uv": [0, 5, 3, 8]}}}

    def kruis():
        rot = {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True}
        return [{"from": [0.8, 0, 8], "to": [15.2, 16, 8], "rotation": rot, "shade": False, "faces": {
                    "north": {"texture": "#blad", "uv": [0, 0, 16, 16]}, "south": {"texture": "#blad", "uv": [0, 0, 16, 16]}}},
                {"from": [8, 0, 0.8], "to": [8, 16, 15.2], "rotation": rot, "shade": False, "faces": {
                    "west": {"texture": "#blad", "uv": [0, 0, 16, 16]}, "east": {"texture": "#blad", "uv": [0, 0, 16, 16]}}}]
    for naam, blad in (("guh_bamboe", None), ("guh_bamboe_klein", "guh_bamboe_blad_klein"), ("guh_bamboe_groot", "guh_bamboe_blad_groot")):
        t = {"stengel": "guhs:block/guh_bamboe", "particle": "guhs:block/guh_bamboe"}
        if blad:
            t["blad"] = f"guhs:block/{blad}"
        w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": t,
                                            "elements": [stengel] + (kruis() if blad else [])})
    w(f"{A}/blockstates/guh_bamboe.json", {"multipart": [
        {"when": {"leaves": "none"}, "apply": {"model": "guhs:block/guh_bamboe"}},
        {"when": {"leaves": "small"}, "apply": {"model": "guhs:block/guh_bamboe_klein"}},
        {"when": {"leaves": "large"}, "apply": {"model": "guhs:block/guh_bamboe_groot"}}]})
    h.item_model("guh_bamboe")
    h.self_drop("guh_bamboe")
    # (for players far from a valley: vanilla bamboo with roze verf)
    h.shapeless("guh_bamboe", ["minecraft:bamboo", "minecraft:pink_dye"], "guhs:guh_bamboe", 1)
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:guh_bamboe"])
    h.add_tag("minecraft/tags/block/sword_efficient", ["guhs:guh_bamboe"])
    h.add_tag("guhs/tags/block/guh_bamboe_grond", ["#minecraft:wool", "#minecraft:dirt", "#minecraft:sand", "guhs:geharkt_zand",
                                                    "guhs:geharkt_zand_ring", "guhs:gladde_knuffelsteen"])


def bonsai(h):
    A, w = h.A, h.w
    pot = [doos(4, 0, 4, 12, 4, 12, "#pot"), doos(3, 3, 3, 13, 5, 13, "#pot"), doos(4, 5, 4, 12, 5.5, 12, "#aarde", ("up",))]
    s, b = "#stam", "#blad"
    vormen = [
        # 0 upright: a straight trunk, three tiers getting smaller
        [doos(7, 5, 7, 9, 10, 9, s), doos(3, 8, 5, 13, 10, 11, b), doos(5, 8, 3, 11, 10, 13, b), doos(5, 10, 5, 11, 12, 11, b),
         doos(6, 12, 6, 10, 14, 10, b)],
        # 1 slanting: the trunk leans east, a pad at the top and a low branch to the west
        [doos(6, 5, 7, 8, 8, 9, s), doos(7, 7, 7, 9, 10, 9, s), doos(8, 9, 7, 10, 12, 9, s), doos(4, 7, 7.5, 7, 8, 8.5, s),
         doos(7, 11, 4, 14, 13, 12, b), doos(9, 13, 6, 13, 14.5, 10, b), doos(1, 7, 6, 5, 9, 10, b)],
        # 2 cascade: up a little, over the rim and down the outside
        [doos(7, 5, 7, 9, 8, 9, s), doos(8, 6, 7, 14, 8, 9, s), doos(13, 2, 7, 15, 7, 9, s), doos(5, 8, 5, 10, 10, 10, b),
         doos(11, 6, 5, 16, 8, 11, b), doos(12, 1, 6, 16, 3, 10, b)],
        # 3 in blossom: a round pink crown
        [doos(7, 5, 7, 9, 9, 9, s), doos(4, 9, 4, 12, 13, 12, b), doos(5, 13, 5, 11, 14, 11, b), doos(3, 10, 5, 13, 12, 11, b),
         doos(5, 10, 3, 11, 12, 13, b)],
    ]
    assert len(vormen) == BONSAI_VORMEN
    variants = {}
    for i, el in enumerate(vormen):
        w(f"{A}/models/block/bonsai_pot_{i}.json", {"parent": "minecraft:block/block", "textures": {
            "pot": "guhs:block/bonsai_pot", "aarde": "guhs:block/bonsai_pot_aarde", "stam": "guhs:block/esdoorn_stam",
            "blad": "guhs:block/bonsai_pot_bloesem" if i == 3 else "guhs:block/bonsai_pot_blad", "particle": "guhs:block/bonsai_pot"},
            "elements": pot + el})
        for f, v in _draai(f"bonsai_pot_{i}").items():
            variants[f"facing={f},vorm={i}"] = v
    w(f"{A}/blockstates/bonsai_pot.json", {"variants": variants})
    w(f"{A}/models/item/bonsai_pot.json", {"parent": "guhs:block/bonsai_pot_0"})
    h.self_drop("bonsai_pot")
    h.shaped("bonsai_pot", ["Z", "P"], {"Z": "#minecraft:saplings", "P": "minecraft:flower_pot"}, "guhs:bonsai_pot", 1)
    h.shaped("bonsai_pot_van_steen", ["SZS", " S "], {"Z": "#minecraft:saplings", "S": "guhs:gladde_knuffelsteen"}, "guhs:bonsai_pot", 1)
    h.add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:bonsai_pot"])


def knuffelsteen(h, z, names):
    A, D, w = h.A, h.D, h.w
    b = "gladde_knuffelsteen"
    h.simple_block(b)
    h.self_drop(b)
    copy_set(h, z, names, {f"{b}_trap": "brick_stairs", f"{b}_plaat": "brick_slab"}, {"minecraft:block/bricks": f"guhs:block/{b}"}, b, b, "bricks")
    w(f"{A}/models/item/{b}_trap.json", {"parent": f"guhs:block/{b}_trap"})
    w(f"{A}/models/item/{b}_plaat.json", {"parent": f"guhs:block/{b}_plaat"})
    # found in the valley's rock faces; far from one: the brick-like knuffelsteen, smoothed in a furnace
    w(f"{D}/recipe/{b}.json", {"type": "minecraft:smelting", "category": "blocks", "ingredient": {"item": "guhs:knuffelsteen"},
                               "result": {"id": f"guhs:{b}"}, "experience": 0.1, "cookingtime": 200})
    for result, n in ((f"{b}_trap", 1), (f"{b}_plaat", 2)):
        w(f"{D}/recipe/{result}_steenzagen.json", {"type": "minecraft:stonecutting", "ingredient": {"item": f"guhs:{b}"},
                                                   "result": {"id": f"guhs:{result}", "count": n}})
    for kind in ("block", "item"):
        h.add_tag(f"minecraft/tags/{kind}/stairs", [f"guhs:{b}_trap"])
        h.add_tag(f"minecraft/tags/{kind}/slabs", [f"guhs:{b}_plaat"])
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{b}", f"guhs:{b}_trap", f"guhs:{b}_plaat"])


# =====================================================================================================================
# esdoorn
# =====================================================================================================================
def esdoorn(h, z):
    A, D, w = h.A, h.D, h.w
    t = {"end": "guhs:block/esdoorn_stam_top", "side": "guhs:block/esdoorn_stam"}
    w(f"{A}/models/block/esdoorn_stam.json", {"parent": "minecraft:block/cube_column", "textures": t})
    w(f"{A}/models/block/esdoorn_stam_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": t})
    w(f"{A}/blockstates/esdoorn_stam.json", {"variants": {
        "axis=y": {"model": "guhs:block/esdoorn_stam"},
        "axis=z": {"model": "guhs:block/esdoorn_stam_horizontal", "x": 90},
        "axis=x": {"model": "guhs:block/esdoorn_stam_horizontal", "x": 90, "y": 90}}})
    w(f"{A}/models/item/esdoorn_stam.json", {"parent": "guhs:block/esdoorn_stam"})
    h.self_drop("esdoorn_stam")
    w(f"{A}/models/block/esdoorn_zaailing.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                  "textures": {"cross": "guhs:block/esdoorn_zaailing"}})
    w(f"{A}/blockstates/esdoorn_zaailing.json", {"variants": {"": {"model": "guhs:block/esdoorn_zaailing"}}})
    h.item_model("esdoorn_zaailing", "guhs:block/esdoorn_zaailing")
    h.self_drop("esdoorn_zaailing")
    kers = json.loads(z.read("data/minecraft/worldgen/configured_feature/cherry.json"))
    for kleur in ("rood", "oranje"):
        b = f"esdoorn_bladeren_{kleur}"
        h.simple_block(b, render_type="minecraft:cutout_mipped")
        # vanilla's leaf loot (shears or silk touch: the leaves; else now and then a sapling or sticks), without the apples
        loot = json.dumps(json.loads(z.read("data/minecraft/loot_table/blocks/dark_oak_leaves.json")))
        loot = json.loads(loot.replace("minecraft:dark_oak_leaves", f"guhs:{b}").replace("minecraft:dark_oak_sapling", "guhs:esdoorn_zaailing"))
        loot["pools"] = [p for p in loot["pools"] if "minecraft:apple" not in json.dumps(p)]
        w(f"{D}/loot_table/blocks/{b}.json", loot)
        # the sapling's tree: vanilla's cherry shape (a forked trunk, a wide crown), a bit lower, in esdoorn
        boom = json.loads(json.dumps(kers))
        c = boom["config"]
        c["trunk_provider"] = {"type": "minecraft:simple_state_provider", "state": {"Name": "guhs:esdoorn_stam", "Properties": {"axis": "y"}}}
        c["foliage_provider"] = {"type": "minecraft:simple_state_provider", "state": {
            "Name": f"guhs:{b}", "Properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}}}
        c["trunk_placer"]["base_height"] = 6
        c["decorators"] = []
        w(f"{D}/worldgen/configured_feature/esdoorn_boom_{kleur}.json", boom)
    add = h.add_tag
    for kind in ("block", "item"):
        add(f"minecraft/tags/{kind}/logs_that_burn", ["guhs:esdoorn_stam"])
        add(f"minecraft/tags/{kind}/leaves", ["guhs:esdoorn_bladeren_rood", "guhs:esdoorn_bladeren_oranje"])
        add(f"minecraft/tags/{kind}/saplings", ["guhs:esdoorn_zaailing"])
    add("minecraft/tags/block/mineable/axe", ["guhs:esdoorn_stam"])
    add("minecraft/tags/block/mineable/hoe", ["guhs:esdoorn_bladeren_rood", "guhs:esdoorn_bladeren_oranje"])


def texts(h):
    lib.teksten(h, {f"block.guhs.{b}": naam for b, naam in NAMEN.items()})
    lib.teksten(h, {f"block.guhs.{b}.lore": tekst for b, tekst in UITLEG.items()})


RECEPTEN = ["roze_lakhout_planken", "roze_lakhout_planken_lakken", "roze_lakhout_balk", *HOUT, "guh_dakpan_wit", "guh_dakpan_wit_steenzagen",
            *[f"guh_dakpan_{k}{s}" for k in KLEUREN for s in ("_verven", "_trap", "_plaat", "_hoek", "_trap_steenzagen", "_plaat_steenzagen",
                                                               "_hoek_steenzagen")],
            "shoji", "tatami", "guh_bamboe_papier", "guh_bamboe_stok", "toro", "geharkt_zand", "geharkt_zand_ring", "geharkt_zand_uit_ring",
            "guh_bamboe", "bonsai_pot", "bonsai_pot_van_steen", "gladde_knuffelsteen", "gladde_knuffelsteen_trap", "gladde_knuffelsteen_plaat",
            "gladde_knuffelsteen_trap_steenzagen", "gladde_knuffelsteen_plaat_steenzagen"]


def selfcheck(h):
    A, D = h.A, h.D
    missing = []
    for b in BLOCKS:
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            missing.append(f"blockstate {b}")
        if not os.path.exists(f"{A}/models/item/{b}.json"):
            missing.append(f"item model {b}")
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
        if not os.path.exists(f"{D}/loot_table/blocks/{b}.json"):
            missing.append(f"loot {b}")
    for r in RECEPTEN:
        if not os.path.exists(f"{D}/recipe/{r}.json"):
            missing.append(f"recipe {r}")
    stems = ("roze_lakhout", "guh_dakpan", "shoji", "tatami", "toro", "geharkt_zand", "guh_bamboe", "bonsai", "gladde_knuffelsteen", "esdoorn")
    for f in os.listdir(f"{A}/models/block"):
        if f.startswith(stems):
            model = json.load(open(os.path.join(A, "models", "block", f), encoding="utf-8"))
            for t in model.get("textures", {}).values():
                if isinstance(t, dict):
                    t = t.get("sprite", "")
                if t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                    missing.append(f"texture {t} ({f})")
            for e in model.get("elements", []):
                for c in e["from"] + e["to"]:
                    if not -16 <= c <= 32:
                        missing.append(f"element out of range in {f}")
    if tex_krul() != (KRUL_BOVEN, KRUL_ONDER):
        missing.append("the krul's numbers differ from DakpanHoekBlock.java")
    if missing:
        raise SystemExit(f"bio_blokken_dal assets missing: {missing}")


def tex_krul():
    """The krul's curl as DakpanHoekBlock.java has it (the model and the collision must be one shape)."""
    import re
    src = open(os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "bio", "blokkendal", "DakpanHoekBlock.java"), encoding="utf-8").read()
    m = re.search(r"BOVEN = \{([0-9, ]+)\}, ONDER = \{([0-9, ]+)\}", src)
    return tuple([int(v) for v in g.split(",")] for g in m.groups())


def build(h):
    tex.build(h)
    with _jar() as z:
        names = z.namelist()
        lakhout(h, z, names)
        dakpannen(h, z, names)
        knuffelsteen(h, z, names)
        esdoorn(h, z)
    shoji_tatami(h)
    toro(h)
    zand(h)
    bamboe(h)
    bonsai(h)
    texts(h)
    selfcheck(h)
