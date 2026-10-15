"""
bbq2 (fossiel-mijn): the Fossiel-opgraving, the Zoutkristalmijn and zoutkristal. Java: feature/fossielmijn.

  zoutkristal         the resource of the "Zout" tier of Guh-technologie (CONTRACT_130 7). Three sources:
                        - the vein in the grotto of the Zoutkristalmijn: every player has an own stock in it that grows back
                          (Zoutmijn.java), so it never runs dry, whoever came before;
                        - guhs:fossielmijn_zoutkristalerts in chunks generated from now on (biome modifier, every biome of
                          the Guhbarbecuether);
                        - now and then in the bottenzand of the Fossiel-opgraving (a daily find).
  Fossiel-opgraving   structure guhs:fossiel_opgraving, the Archeoloog-guh, questline "archeoloog": brush five bones out
                      of the bottenzand and put the little Tyrannoguhrus Njex together on the stand. Everything is per
                      player: the bottenzand and the stand never change in the world, each player sees their own.
  Zoutkristalmijn     structure guhs:zoutkristalmijn, the Mijnwerker-guh, questline "mijnwerker": hack the fallen rock off
                      the cart track (it comes back for the next player) and find the vein.

Split: fossiel_mijn_tex.py (textures), fossiel_mijn_bouw.py (the two templates and their self-check), fossiel_mijn_wiki.py
(the wiki entries). Dutch only (CONTRACT_130 1); English names are proposed in the slice report.
"""
import json
import os

import numpy as np
from PIL import Image

from features import bbq2, verhaal_motor, wereld
from features import fossiel_mijn_bouw as bouw
from features import fossiel_mijn_tex as tex
from features import sterrenwacht_hulp as hulp

BOTTEN = ["schedel", "ruggengraat", "ribben", "pootjes", "staart"]      # (FossielmijnFeature.BOTTEN, in this order)
BLOKKEN = ["fossielmijn_zoutkristalerts", "fossielmijn_zoutader", "fossielmijn_zoutkristalblok", "fossielmijn_zoutkristalletjes",
           "fossielmijn_bottenzand", "fossielmijn_puin", "fossielmijn_skeletrek", "fossielmijn_fossielbeeldje"]
ITEMS = ["zoutkristal"] + [f"fossielmijn_bot_{b}" for b in BOTTEN] + ["fossielmijn_kwastje", "fossielmijn_zoutkristalhouweel"]
# salts: CONTRACT_130 3, slice 11: 213011NN
SALT_OPGRAVING, SALT_MIJN = 21301101, 21301111

FTB_SECTIES = [("fossiel_mijn_opgraving", "De Fossiel-opgraving", "npc:archeoloogguh",
                ["fossiel_mijn_opgraving_vind", "fossiel_mijn_archeoloog_1", "fossiel_mijn_archeoloog_2", "fossiel_mijn_archeoloog_3",
                 "fossiel_mijn_archeoloog_4", "fossiel_mijn_dagvondst", "fossiel_mijn_beeldje"]),
               ("fossiel_mijn_mijn", "De Zoutkristalmijn", "npc:mijnwerkerguh", None)]


# =====================================================================================================================
# textures
# =====================================================================================================================
def textures(h):
    save = h.save
    save(tex.zoutkristalerts(), "block", "fossielmijn_zoutkristalerts.png")
    save(tex.zoutader(), "block", "fossielmijn_zoutader.png")
    save(tex.zoutkristalblok(), "block", "fossielmijn_zoutkristalblok.png")
    save(tex.zoutkristalletjes(), "block", "fossielmijn_zoutkristalletjes.png")
    save(tex.bottenzand(), "block", "fossielmijn_bottenzand.png")
    save(tex.bottenzand(leeg=True), "block", "fossielmijn_bottenzand_leeg.png")
    save(tex.puin(), "block", "fossielmijn_puin.png")
    save(tex.bot(), "block", "fossielmijn_bot.png")
    save(tex.schedel(), "block", "fossielmijn_schedel.png")
    save(tex.rek(), "block", "fossielmijn_rek.png")
    for name, rows in tex.ICONS.items():
        assert len(rows) == 16 and all(len(r) == 16 for r in rows), f"fossiel_mijn icon {name}: 16 x 16"
        save(h.grid(rows, tex.ITEM_PAL), "item", f"{name}.png")


# =====================================================================================================================
# models: the little Tyrannoguhrus Njex (the stand, its five parts, the statuette), the rubble, the plain blocks
# =====================================================================================================================
def _doos(x0, y0, z0, x1, y1, z1, tex_ref, voor=None):
    """A model element with explicit uv on every face (faces above y 16 or outside the block need it: CONTRACT_130 9).
    voor = (texture, [u0, v0, u1, v1]): another texture on the north face (the skull's face)."""
    def uv(a0, a1, b0, b1):
        w, hgt = min(16.0, a1 - a0), min(16.0, b1 - b0)
        u0 = max(0.0, min(16.0 - w, a0 % 16))
        v0 = max(0.0, min(16.0 - hgt, (16 - b1) % 16))
        return [round(u0, 3), round(v0, 3), round(u0 + w, 3), round(v0 + hgt, 3)]
    faces = {"north": {"uv": uv(x0, x1, y0, y1), "texture": tex_ref}, "south": {"uv": uv(x0, x1, y0, y1), "texture": tex_ref},
             "west": {"uv": uv(z0, z1, y0, y1), "texture": tex_ref}, "east": {"uv": uv(z0, z1, y0, y1), "texture": tex_ref},
             "up": {"uv": uv(x0, x1, z0, z1), "texture": tex_ref}, "down": {"uv": uv(x0, x1, z0, z1), "texture": tex_ref}}
    if voor:
        faces["north"] = {"uv": voor[1], "texture": voor[0]}
    return {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}


def _spiegel(dozen):
    """The same boxes mirrored in x (left and right legs, ribs, ears)."""
    out = []
    for (x0, y0, z0, x1, y1, z1) in dozen:
        out.append((x0, y0, z0, x1, y1, z1))
        out.append((16 - x1, y0, z0, 16 - x0, y1, z1))
    return out


# the skeleton in model pixels, looking north (-z): x 0..16 is the stand's block, it sticks out to z -9 and z 26, y to 27
SKELET = {
    "pootjes": _spiegel([(3.5, 4.5, 8.5, 6.0, 11.0, 11.0),          # the thighs
                         (4.0, 2.5, 8.0, 5.5, 4.5, 10.0),           # the shins
                         (3.5, 2.0, 4.5, 6.0, 3.0, 10.5),           # the big feet
                         (3.5, 2.0, 3.5, 4.3, 2.8, 4.5),            # two toes
                         (5.2, 2.0, 3.5, 6.0, 2.8, 4.5)]) + [(4.0, 10.0, 8.5, 12.0, 12.0, 11.5)],   # and the hips
    "ruggengraat": [(7.0, 11.5, 9.0, 9.0, 13.5, 11.5), (7.0, 12.5, 6.5, 9.0, 14.5, 9.0), (7.0, 13.5, 4.0, 9.0, 15.5, 6.5),
                    (7.0, 14.5, 1.5, 9.0, 16.5, 4.0), (7.0, 15.5, -0.5, 9.0, 17.5, 1.5),
                    (7.5, 13.5, 9.6, 8.5, 15.0, 10.6), (7.5, 14.5, 7.2, 8.5, 16.0, 8.2), (7.5, 15.5, 4.8, 8.5, 17.0, 5.8),
                    (7.5, 16.5, 2.3, 8.5, 18.0, 3.3)],
    "ribben": _spiegel([(3.0, 7.5, 7.0, 4.0, 13.0, 8.0), (4.0, 12.5, 7.0, 7.0, 13.5, 8.0), (4.0, 7.0, 7.0, 5.5, 8.0, 8.0),
                        (3.0, 8.5, 4.6, 4.0, 14.0, 5.6), (4.0, 13.5, 4.6, 7.0, 14.5, 5.6), (4.0, 8.0, 4.6, 5.5, 9.0, 5.6),
                        (3.5, 10.0, 2.2, 4.5, 15.0, 3.2), (4.5, 14.5, 2.2, 7.0, 15.5, 3.2), (4.5, 9.5, 2.2, 5.8, 10.5, 3.2),
                        (2.5, 11.0, 0.6, 3.5, 13.5, 1.6), (2.5, 10.2, -0.6, 3.5, 11.2, 0.8)]),   # the tiny arms
    "staart": [(7.0, 10.5, 11.5, 9.0, 12.5, 14.5), (7.0, 9.5, 14.5, 9.0, 11.5, 17.5), (7.2, 8.3, 17.5, 8.8, 10.0, 20.5),
               (7.4, 7.2, 20.5, 8.6, 8.6, 23.5), (7.6, 6.4, 23.5, 8.4, 7.4, 26.0),
               (7.5, 12.5, 12.4, 8.5, 13.6, 13.4), (7.5, 11.5, 15.4, 8.5, 12.5, 16.4)],
    "schedel": [(2.5, 15.0, -9.0, 13.5, 24.0, 0.0),                 # the big round head
                (3.5, 13.4, -8.5, 12.5, 15.0, -1.5)]                # the jaw, hanging open a little
               + _spiegel([(0.5, 21.5, -6.5, 4.0, 26.0, -5.0), (1.5, 26.0, -6.5, 3.5, 27.0, -5.0)]),   # the ears
}
REK_DOZEN = [(1.0, 0.0, 2.0, 15.0, 2.0, 14.0)]                      # the plinth
STANGEN = [(7.5, 2.0, 4.5, 8.5, 13.5, 5.5), (7.5, 2.0, 10.0, 8.5, 11.5, 11.0)]   # the two brass rods that carry it


def _elementen(deel, schaal=None):
    """The model elements of a part. schaal = (factor, y0): shrunk around the block's middle for the statuette."""
    bot, kop = "#bot", "#schedel"
    out = []
    for i, d in enumerate(SKELET[deel]):
        if schaal:
            f, lift = schaal
            d = (8 + (d[0] - 8) * f, lift + (d[1] - 2) * f, 8 + (d[2] - 8.5) * f, 8 + (d[3] - 8) * f, lift + (d[4] - 2) * f, 8 + (d[5] - 8.5) * f)
            d = tuple(round(v, 3) for v in d)
        voor = None
        if deel == "schedel" and i == 0:
            voor = (kop, [2.5, 2, 13.5, 13])                        # the eye sockets and the nose
        elif deel == "schedel" and i == 1:
            voor = (kop, [3.5, 13, 12.5, 16])                       # the teeth
        out.append(_doos(*d, bot, voor))
    return out


def modellen(h):
    A = h.A
    w = h.w
    t = {"particle": "guhs:block/fossielmijn_bot", "bot": "guhs:block/fossielmijn_bot", "schedel": "guhs:block/fossielmijn_schedel",
         "rek": "guhs:block/fossielmijn_rek", "messing": "minecraft:block/gold_block"}
    # the stand (always there) and one model per part
    w(f"{A}/models/block/fossielmijn_skeletrek.json", {"parent": "minecraft:block/block", "textures": {**t, "particle": "guhs:block/fossielmijn_rek"},
                                                       "elements": [_doos(*d, "#rek") for d in REK_DOZEN] + [_doos(*d, "#messing") for d in STANGEN]})
    for deel in BOTTEN:
        w(f"{A}/models/block/fossielmijn_skelet_{deel}.json", {"textures": t, "elements": _elementen(deel)})
    parts = []
    for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        def apply(model):
            a = {"model": f"guhs:block/{model}"}
            if rot:
                a["y"] = rot
            return a
        parts.append({"when": {"facing": facing}, "apply": apply("fossielmijn_skeletrek")})
        for deel in BOTTEN:
            parts.append({"when": {"facing": facing, deel: "true"}, "apply": apply(f"fossielmijn_skelet_{deel}")})
    w(f"{A}/blockstates/fossielmijn_skeletrek.json", {"multipart": parts})
    # in the hand / the inventory: the stand with the whole skeleton on it, small
    alles = [_doos(*d, "#rek") for d in REK_DOZEN] + [_doos(*d, "#messing") for d in STANGEN] + [e for deel in BOTTEN for e in _elementen(deel)]
    w(f"{A}/models/item/fossielmijn_skeletrek.json", {"parent": "minecraft:block/block", "textures": t, "elements": alles, "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, -2.2, 0], "scale": [0.36, 0.36, 0.36]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]}}})
    # the statuette: the same skeleton, shrunk onto a stone plinth
    klein = (0.4, 3.2)
    beeldje = [_doos(3.0, 0.0, 3.0, 13.0, 2.0, 13.0, "#steen"), _doos(4.0, 2.0, 4.0, 12.0, 3.2, 12.0, "#steen"),
               _doos(5.0, 0.6, 2.9, 11.0, 1.6, 3.0, "#messing")]
    beeldje += [e for deel in BOTTEN for e in _elementen(deel, klein)]
    w(f"{A}/models/block/fossielmijn_fossielbeeldje.json", {"parent": "minecraft:block/block", "textures": {
        **t, "steen": "guhs:block/gepolijst_roosterijzer", "particle": "guhs:block/gepolijst_roosterijzer"}, "elements": beeldje})
    w(f"{A}/blockstates/fossielmijn_fossielbeeldje.json", {"variants": h.facing_states("fossielmijn_fossielbeeldje")})
    w(f"{A}/models/item/fossielmijn_fossielbeeldje.json", {"parent": "guhs:block/fossielmijn_fossielbeeldje", "display": {
        "gui": {"rotation": [20, 215, 0], "translation": [0, 0.5, 0], "scale": [0.9, 0.9, 0.9]}}})

    # the rubble: lumps of rock over a cart rail (axis = the way the rail runs)
    lumps = [(1, 0, 2, 9, 7, 10), (7, 0, 6, 15, 10, 14), (3, 0, 9, 10, 5, 15), (9, 0, 1, 14, 4, 7), (5, 7, 5, 10, 11, 10), (0, 0, 11, 4, 3, 15)]
    w(f"{A}/models/block/fossielmijn_puin.json", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": {"particle": "guhs:block/fossielmijn_puin", "puin": "guhs:block/fossielmijn_puin", "rail": "minecraft:block/rail"},
        "elements": [{"from": [0, 1, 0], "to": [16, 1, 16], "faces": {"down": {"uv": [0, 16, 16, 0], "texture": "#rail"},
                                                                         "up": {"uv": [0, 0, 16, 16], "texture": "#rail"}}}]
        + [_doos(*d, "#puin") for d in lumps]})
    w(f"{A}/blockstates/fossielmijn_puin.json", {"variants": {"axis=z": {"model": "guhs:block/fossielmijn_puin"},
                                                              "axis=x": {"model": "guhs:block/fossielmijn_puin", "y": 90}}})
    w(f"{A}/models/item/fossielmijn_puin.json", {"parent": "guhs:block/fossielmijn_puin"})

    # the plain ones
    for b in ("fossielmijn_zoutkristalerts", "fossielmijn_zoutader", "fossielmijn_zoutkristalblok"):
        h.simple_block(b)
    w(f"{A}/models/block/fossielmijn_zoutkristalletjes.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                               "textures": {"cross": "guhs:block/fossielmijn_zoutkristalletjes"}})
    w(f"{A}/blockstates/fossielmijn_zoutkristalletjes.json", {"variants": {"": {"model": "guhs:block/fossielmijn_zoutkristalletjes"}}})
    h.item_model("fossielmijn_zoutkristalletjes", "guhs:block/fossielmijn_zoutkristalletjes")
    # bottenzand: ash on the sides, the bone tip (or the brushed dent) on top
    for leeg in (False, True):
        naam = "fossielmijn_bottenzand" + ("_leeg" if leeg else "")
        w(f"{A}/models/block/{naam}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": f"guhs:block/{naam}", "bottom": "guhs:block/as_aarde", "side": "guhs:block/as_aarde"}})
    w(f"{A}/blockstates/fossielmijn_bottenzand.json", {"variants": {"leeg=false": {"model": "guhs:block/fossielmijn_bottenzand"},
                                                                    "leeg=true": {"model": "guhs:block/fossielmijn_bottenzand_leeg"}}})
    w(f"{A}/models/item/fossielmijn_bottenzand.json", {"parent": "guhs:block/fossielmijn_bottenzand"})
    for i in ITEMS:
        h.item_model(i, parent="minecraft:item/handheld" if i in ("fossielmijn_kwastje", "fossielmijn_zoutkristalhouweel") else "minecraft:item/generated")


# =====================================================================================================================
# loot, recipes, tags
# =====================================================================================================================
def data(h):
    D = h.D
    silk = [{"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
        {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]
    h.w(f"{D}/loot_table/blocks/fossielmijn_zoutkristalerts.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "guhs:fossielmijn_zoutkristalerts", "conditions": silk},
            {"type": "minecraft:item", "name": "guhs:zoutkristal", "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"function": "minecraft:explosion_decay"}]}]}]}]})
    for b in ("fossielmijn_zoutkristalblok", "fossielmijn_zoutkristalletjes", "fossielmijn_fossielbeeldje"):
        h.self_drop(b)
    # (the vein, the bottenzand, the rubble and the stand are part of their building: they never drop anything)
    for b in ("fossielmijn_zoutader", "fossielmijn_bottenzand", "fossielmijn_puin", "fossielmijn_skeletrek"):
        h.w(f"{D}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": []})
    # the barrels: modest
    loot = {
        "fossielmijn_opgraving": [("minecraft:bone", 5, 1, 3), ("minecraft:bone_meal", 3, 1, 4), ("guhs:kaas_knabbels", 5, 2, 6),
                                  ("minecraft:paper", 3, 1, 3), ("minecraft:string", 2, 1, 2), ("guhs:verkoold_guhbot", 2, 1, 2),
                                  ("minecraft:brush", 1, 1, 1)],
        "fossielmijn_mijn": [("guhs:zoutkristal", 4, 1, 2), ("guhs:kaas_knabbels", 5, 2, 6), ("minecraft:rail", 3, 2, 5),
                             ("minecraft:charcoal", 3, 1, 4), ("minecraft:lantern", 2, 1, 1), ("guhs:houtskoolsteen", 3, 2, 6)],
    }
    for table, entries in loot.items():
        h.w(f"{D}/loot_table/chests/{table}.json", {"type": "minecraft:chest", "pools": [
            {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
                {"type": "minecraft:item", "name": item, "weight": wgt, "functions": h.count_fn(lo, hi)} for item, wgt, lo, hi in entries]}]})
    # recipes: the crystal as a building block and a little lamp; an ore block someone brought home melts down to its salt
    h.shaped("fossielmijn_zoutkristalblok", ["ZZ", "ZZ"], {"Z": "guhs:zoutkristal"}, "guhs:fossielmijn_zoutkristalblok")
    h.shapeless("zoutkristal_uit_blok", ["guhs:fossielmijn_zoutkristalblok"], "guhs:zoutkristal", 4)
    h.shapeless("fossielmijn_zoutkristalletjes", ["guhs:zoutkristal", "guhs:zoutkristal"], "guhs:fossielmijn_zoutkristalletjes", 2)
    for kind, time in (("smelting", 200), ("blasting", 100)):
        h.w(f"{D}/recipe/zoutkristal_{kind}.json", {"type": f"minecraft:{kind}", "category": "misc",
                                                    "ingredient": {"item": "guhs:fossielmijn_zoutkristalerts"},
                                                    "result": {"id": "guhs:zoutkristal"}, "experience": 0.2, "cookingtime": time})
    # tags
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{b}" for b in (
        "fossielmijn_zoutkristalerts", "fossielmijn_zoutader", "fossielmijn_zoutkristalblok", "fossielmijn_zoutkristalletjes",
        "fossielmijn_puin", "fossielmijn_skeletrek", "fossielmijn_fossielbeeldje")])
    h.add_tag("minecraft/tags/block/mineable/shovel", ["guhs:fossielmijn_bottenzand"])
    h.add_tag("minecraft/tags/block/needs_stone_tool", ["guhs:fossielmijn_zoutkristalerts", "guhs:fossielmijn_zoutader"])
    h.add_tag("minecraft/tags/item/pickaxes", ["guhs:fossielmijn_zoutkristalhouweel"])
    # (the cave carver cuts through the ore like through the rock around it)
    h.add_tag("guhs/tags/block/barbecuegrot_vervangbaar", ["guhs:fossielmijn_zoutkristalerts"])
    # the fossil bones are quest items: they never go into a Bank Guh or a chest of a helper
    h.add_tag("guhs/tags/item/loaned", [f"guhs:fossielmijn_bot_{b}" for b in BOTTEN])
    h.add_tag("guhs/tags/item/fossielmijn_botten", [f"guhs:fossielmijn_bot_{b}" for b in BOTTEN])


# =====================================================================================================================
# worldgen: the ore (new chunks only: an ore never appears in a chunk that exists), the two structures
# =====================================================================================================================
def erts(h):
    D = h.D
    h.w(f"{D}/worldgen/configured_feature/fossielmijn_zoutkristalerts.json", {"type": "minecraft:ore", "config": {
        "size": 8, "discard_chance_on_air_exposure": 0.0, "targets": [
            {"target": {"predicate_type": "minecraft:block_match", "block": "guhs:houtskoolsteen"},
             "state": {"Name": "guhs:fossielmijn_zoutkristalerts"}}]}})
    h.w(f"{D}/worldgen/placed_feature/fossielmijn_zoutkristalerts.json", {"feature": "guhs:fossielmijn_zoutkristalerts", "placement": [
        {"type": "minecraft:count", "count": 7}, {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"above_bottom": 10},
                                                      "max_inclusive": {"below_top": 10}}},
        {"type": "minecraft:biome"},
        {"type": "guhs:buiten_gebouwen"}]})          # (never inside a building: make_v2.buiten_gebouwen only knows the biomes' own lists)
    h.w(f"{D}/neoforge/biome_modifier/fossielmijn_zoutkristalerts.json", {
        "type": "neoforge:add_features", "biomes": "#guhs:is_barbecuether", "features": "guhs:fossielmijn_zoutkristalerts",
        "step": "underground_decoration"})


def structuren(h):
    problems, bouwsels = bouw.build_all(h)
    if problems:
        print("fossiel_mijn geometry check found problems:\n  " + "\n  ".join(problems[:40]))
        raise SystemExit("fossiel_mijn: fix the templates (see the geometry check above)")
    wereld.bbq_structuur(h, bouw.OPGRAVING, soort="grot", titel="Fossiel-opgraving",
                         tooltip="Een opgraving met het skelet van een reuzenguh, en de Archeoloog-guh (Guhbarbecuether)",
                         biomes=["asdal", "houtskoolvlakte", "rookdelta"], salt=SALT_OPGRAVING, templates=[(bouw.OPGRAVING, 1)],
                         spacing=30, separation=12, gegarandeerd=dict(sector=4, min=250, max=900), grootte=28, vlak=8, hoogte=12)
    wereld.bbq_structuur(h, bouw.MIJN, soort="grot", titel="Zoutkristalmijn",
                         tooltip="Een berg van zout met een mijn erin: hier zit de kristalader, en de Mijnwerker-guh (Guhbarbecuether)",
                         biomes=wereld.BBQ, salt=SALT_MIJN, templates=[(bouw.MIJN, 1)],
                         spacing=24, separation=9, gegarandeerd=dict(sector=5, min=250, max=900), grootte=30, vlak=8, hoogte=14)
    # the centre jigsaw is in layer 0 and the pool says where the ground really is (fossiel_mijn_bouw's module text): the
    # floor lands on the cave floor, the terrain is smoothed towards it, and the pit and the grotto reach below it
    for naam, b in bouwsels.items():
        def grond(pool, delta=b.G + 1):
            for e in pool["elements"]:
                el = {"element_type": "guhs:grond_single_pool_element"}
                el.update({k: v for k, v in e["element"].items() if k not in ("element_type", "ground_level_delta")})
                el["ground_level_delta"] = delta
                e["element"] = el
        h.patch_json(f"{h.D}/worldgen/template_pool/{naam}/start.json", grond)
    return bouwsels


# =====================================================================================================================
# the two NPCs: the sitting guh with a hat of its own
# =====================================================================================================================
def npcs(h):
    c = hulp.cube
    # the Archeoloog-guh: sand-coloured, a pith helmet with a brim front and back, a neckerchief, a moustache
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_archeoloogguh")
    sw = hulp.swatches(geo, ["helm", "band", "doek", "snor"])
    geo["bones"].append({"name": "archeoloog_helm", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        c([-6.6, 24.2, -7.2], [13.2, 2.4, 12.8], sw["helm"]), c([-5.0, 26.6, -5.6], [10.0, 1.4, 9.6], sw["helm"]),
        c([-3.0, 28.0, -3.6], [6.0, 0.7, 5.6], sw["helm"]), c([-0.8, 28.7, -1.6], [1.6, 0.6, 1.6], sw["band"]),
        c([-6.0, 23.9, -9.6], [12.0, 0.7, 2.6], sw["helm"]), c([-6.0, 23.9, 5.4], [12.0, 0.7, 2.2], sw["helm"]),
        c([-6.8, 24.2, -7.4], [13.6, 0.9, 13.2], sw["band"])]})
    geo["bones"].append({"name": "archeoloog_doek", "parent": "body", "pivot": [0, 13, 0], "cubes": [
        c([-5.2, 11.4, -4.6], [10.4, 1.8, 8.4], sw["doek"], inflate=0.1), c([-1.4, 9.4, -4.9], [2.8, 2.2, 0.6], sw["doek"])]})
    geo["bones"].append({"name": "archeoloog_snor", "parent": "head", "pivot": [0, 16, -7.6], "cubes": [
        c([-3.2, 15.3, -7.9], [2.8, 1.0, 0.6], sw["snor"]), c([0.4, 15.3, -7.9], [2.8, 1.0, 0.6], sw["snor"])]})
    hulp.save_geo(h, "guh_npc_archeoloogguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.09, sat=0.75, val=1.0)
    rng = np.random.default_rng(31141)
    hulp.paint_swatch(a, sw["helm"], (214, 196, 150), rng, 8)
    hulp.paint_swatch(a, sw["band"], (120, 84, 48), rng, 6)
    hulp.paint_swatch(a, sw["doek"], (200, 60, 50), rng, 6)
    hulp.paint_swatch(a, sw["snor"], (240, 236, 226), rng, 5)
    h.save(Image.fromarray(a), "entity", "npc_archeoloogguh.png")

    # the Mijnwerker-guh: dusty grey-blue, a yellow helmet with a lamp, a belt
    geo_file, geo = hulp.sitting_geo(h, "geometry.guh_npc_mijnwerkerguh")
    sw = hulp.swatches(geo, ["helm", "lamp", "riem"])
    geo["bones"].append({"name": "mijnwerker_helm", "parent": "head", "pivot": [0, 26, -1], "cubes": [
        c([-6.7, 24.0, -7.3], [13.4, 2.6, 13.0], sw["helm"]), c([-5.4, 26.6, -6.0], [10.8, 1.3, 10.4], sw["helm"]),
        c([-1.0, 27.9, -6.0], [2.0, 0.6, 10.4], sw["helm"]),                    # the ridge over the top
        c([-5.6, 23.8, -9.2], [11.2, 0.7, 2.2], sw["helm"]),                    # a short peak
        c([-1.7, 25.0, -8.5], [3.4, 2.6, 1.4], sw["riem"]), c([-1.2, 25.5, -9.0], [2.4, 1.6, 0.6], sw["lamp"])]})
    geo["bones"].append({"name": "mijnwerker_riem", "parent": "body", "pivot": [0, 8, 0], "cubes": [
        c([-5.4, 6.4, -4.8], [10.8, 1.4, 8.8], sw["riem"], inflate=0.1), c([-1.0, 6.2, -5.2], [2.0, 1.8, 0.6], sw["lamp"])]})
    hulp.save_geo(h, "guh_npc_mijnwerkerguh.geo.json", geo_file)
    a = hulp.sitting_texture(h, hue=0.60, sat=0.42, val=0.86)
    rng = np.random.default_rng(31142)
    hulp.paint_swatch(a, sw["helm"], (246, 196, 40), rng, 7)

    def gloed(block):
        block[8:24, 8:24, :3] = (255, 250, 200)
    hulp.paint_swatch(a, sw["lamp"], (255, 226, 120), rng, 4, gloed)
    hulp.paint_swatch(a, sw["riem"], (64, 52, 46), rng, 5)
    h.save(Image.fromarray(a), "entity", "npc_mijnwerkerguh.png")


# =====================================================================================================================
# texts
# =====================================================================================================================
LANG = {
    # blocks and items
    "item.guhs.zoutkristal": "Zoutkristal",
    "item.guhs.zoutkristal.lore": "Roze, korrelig en lekker hartig. Je maakt er de slimme machines van de Guh-technologie mee.",
    "block.guhs.fossielmijn_zoutkristalerts": "Zoutkristalerts",
    "block.guhs.fossielmijn_zoutader": "Zoutkristalader",
    "block.guhs.fossielmijn_zoutkristalblok": "Zoutkristalblok",
    "block.guhs.fossielmijn_zoutkristalletjes": "Zoutkristalletjes",
    "block.guhs.fossielmijn_bottenzand": "Bottenzand",
    "block.guhs.fossielmijn_puin": "Puin op het spoor",
    "block.guhs.fossielmijn_skeletrek": "Skeletrek",
    "block.guhs.fossielmijn_fossielbeeldje": "Tyrannoguhrus-beeldje",
    "block.guhs.fossielmijn_fossielbeeldje.lore": "Tyrannoguhrus Njex, de koning der knabbelaars. Miljoenen jaren oud en nog steeds trek.",
    "block.guhs.fossielmijn_fossielbeeldje.klik": "Njex!",
    "item.guhs.fossielmijn_bot_schedel": "Tyrannoguhrus-schedel",
    "item.guhs.fossielmijn_bot_ruggengraat": "Tyrannoguhrus-ruggengraat",
    "item.guhs.fossielmijn_bot_ribben": "Tyrannoguhrus-ribben",
    "item.guhs.fossielmijn_bot_pootjes": "Tyrannoguhrus-pootjes",
    "item.guhs.fossielmijn_bot_staart": "Tyrannoguhrus-staartje",
    **{f"item.guhs.fossielmijn_bot_{b}.lore": "Hoort op het rek bij de Fossiel-opgraving" for b in BOTTEN},
    "item.guhs.fossielmijn_kwastje": "Guhkwastje",
    "item.guhs.fossielmijn_kwastje.lore": "Houd rechtsklik ingedrukt op bottenzand. Elke dag ligt er wel weer iets in het zand, njeg!",
    "item.guhs.fossielmijn_zoutkristalhouweel": "Zoutkristalhouweel",
    "item.guhs.fossielmijn_zoutkristalhouweel.lore": "Hakt twee keer zoveel zout uit de kristalader",
    # the dig
    "gui.guhs.fossielmijn.eerst_praten": "Vraag eerst de Archeoloog-guh wat je hier mag doen, njeg",
    "gui.guhs.fossielmijn.vond": "Je kwast iets los: %s! (%s/5)",
    "gui.guhs.fossielmijn.alle_botten": "Dat zijn ze alle vijf! Zet ze op het rek onder het afdakje",
    "gui.guhs.fossielmijn.gekwast": "Hier heb je al gekwast. Zoek een ander plekje bottenzand, njeg",
    "gui.guhs.fossielmijn.dagvondst": "In het zand zat: %s",
    "gui.guhs.fossielmijn.dag_leeg": "Dit plekje is voor vandaag leeg. Morgen ligt er weer iets, njeg",
    "gui.guhs.fossielmijn.rek.leeg": "Een leeg rek. De Archeoloog-guh weet er vast meer van",
    "gui.guhs.fossielmijn.rek.geen": "Hier komt de Tyrannoguhrus Njex. Je hebt nu geen bot bij je dat er nog op moet",
    "gui.guhs.fossielmijn.rek.gezet": "%s zit erop! (%s/5)",
    "gui.guhs.fossielmijn.rek.af": "Hij staat! Vertel het de Archeoloog-guh. VAHOEG!",
    "gui.guhs.fossielmijn.rek.klaar": "Jouw Tyrannoguhrus Njex. Wat een oortjes!",
    "quest.guhs.fossielmijn.archeoloog.hallo1": "Njeg! Voorzichtig waar je loopt! Hier ligt de vondst van de eeuw: een echte Tyrannoguhrus Njex!",
    "quest.guhs.fossielmijn.archeoloog.hallo2": "De grote blijft liggen, maar in het bottenzand zit nog een kleintje. Hier, een Guhkwastje: kwast vijf botten voor me los!",
    "quest.guhs.fossielmijn.archeoloog.kwast_weer": "Kwastje kwijt? Njeg njeg. Hier is een nieuwe. Niet opeten!",
    "quest.guhs.fossielmijn.archeoloog.zoek": "Je hebt %s van de 5 botten. Zoek het bottenzand in de put: daar steekt een puntje bot uit de as.",
    "quest.guhs.fossielmijn.archeoloog.bouw": "Vahoeg, alle vijf! Zet ze nu op het rek onder het afdakje. Er zitten er %s van de 5 op.",
    "quest.guhs.fossielmijn.archeoloog.bot_weer": "Was je een bot kwijt? Gelukkig had ik er een gipsafdruk van. Hier!",
    "quest.guhs.fossielmijn.archeoloog.klaar1": "VAHOEG! Hij staat! Kijk nou toch: die oortjes, die tandjes... Tyrannoguhrus Njex, de koning der knabbelaars!",
    "quest.guhs.fossielmijn.archeoloog.klaar2": "Dit beeldje is voor jou. En houd het kwastje maar: in het bottenzand ligt elke dag wel weer iets.",
    "quest.guhs.fossielmijn.archeoloog.tip0": "De Tyrannoguhrus kon met zijn korte pootjes niet bij zijn eigen knabbels. Daarom is hij uitgestorven. Denk ik. Njeg.",
    "quest.guhs.fossielmijn.archeoloog.tip1": "Elke dag duwt de as weer iets nieuws omhoog. Kom gerust kwasten!",
    "quest.guhs.fossielmijn.archeoloog.tip2": "Ik zoek nog een Guhceratops. Zie je een bot met drie hoorntjes? Roepen!",
    "quest.guhs.fossielmijn.archeoloog.tip3": "Niet knabbelen aan de vondsten. Dat geldt ook voor mij. Vooral voor mij.",
    # the mine
    "gui.guhs.fossielmijn.puin": "Puin weg! (%s/5)",
    "gui.guhs.fossielmijn.puin_klaar": "Het spoor is vrij! Volg het naar de grot met de kristalader",
    "gui.guhs.fossielmijn.ader.eerst": "De Mijnwerker-guh wil eerst dat het spoor vrij is, njeg",
    "gui.guhs.fossielmijn.ader.houweel": "Daar heb je een houweel voor nodig",
    "gui.guhs.fossielmijn.ader.gevonden": "De kristalader! Vahoeg! Breng de Mijnwerker-guh drie zoutkristallen",
    "gui.guhs.fossielmijn.ader.hak": "+%s zoutkristal (nog %s in de ader voor jou)",
    "gui.guhs.fossielmijn.ader.op": "De ader is voor jou even op. Zout groeit vanzelf weer aan: kom straks terug, njeg",
    "gui.guhs.fossielmijn.beloning.ader": "De kristalader: jouw eigen voorraad zout, die altijd weer aangroeit",
    "quest.guhs.fossielmijn.mijnwerker.hallo1": "Njeg, wat een puinhoop! Het dak kwam naar beneden en nu ligt mijn hele karrenspoor vol puin.",
    "quest.guhs.fossielmijn.mijnwerker.hallo2": "Mijn rug doet het niet meer. Hak jij vijf brokken puin van het spoor? Dan vertel ik je waar het zout zit!",
    "quest.guhs.fossielmijn.mijnwerker.puin": "Nog %s brokken puin. Ze liggen op het spoor, in de gangen van de berg. Hakken maar! Is het "
                                              "spoor net door een ander leeggehakt? Wacht een minuutje, njeg: het dak blijft brokkelen.",
    "quest.guhs.fossielmijn.mijnwerker.ader": "Vahoeg, het spoor is vrij! Volg het tot in de grot aan het eind: daar zit de kristalader. Hak er maar eens in!",
    "quest.guhs.fossielmijn.mijnwerker.breng": "Je hebt de ader gevonden! Breng me drie zoutkristallen voor op mijn boterham. Je hebt er nu %s.",
    "quest.guhs.fossielmijn.mijnwerker.klaar1": "Mmm, zout! Zonder zout smaakt een knabbel nergens naar. VAHOEG!",
    "quest.guhs.fossielmijn.mijnwerker.klaar2": "Deze Zoutkristalhouweel is voor jou: daarmee hak je twee keer zoveel. En de ader? Die groeit altijd weer aan. Hak maar raak!",
    "quest.guhs.fossielmijn.mijnwerker.voorraad": "De ader heeft voor jou nu %s kristallen klaarliggen. Zout groeit vanzelf weer aan, njeg.",
    "quest.guhs.fossielmijn.mijnwerker.tip0": "Geen zout, geen smaak. Geen smaak, geen guh. Zo simpel is het.",
    "quest.guhs.fossielmijn.mijnwerker.tip1": "Met zoutkristal maak je slimme dingen: buizen, sensoren, een Hapluikje... Vraag het de Uitvinder-guh maar.",
    "quest.guhs.fossielmijn.mijnwerker.tip2": "Zoutkristalerts zit ook gewoon in de rotsen hier. Maar alleen waar nog nooit iemand geweest is, njeg.",
    # the op command (dev checks, AutoCheck)
    "gui.guhs.fossielmijn.commando.stand": "Fossiel-mijn van %s: botten gevonden %s, op het rek %s, puin %s, zout in de ader %s",
    "gui.guhs.fossielmijn.commando.gezet": "Fossiel-mijn: gezet",
}


def teksten(h):
    for key, text in LANG.items():
        h.lang(key, text, text)
    opgraving = "De Fossiel-opgraving in de Guhbarbecuether"
    verhaal_motor.verhaallijn(
        h, "archeoloog", "De Tyrannoguhrus Njex",
        "De Archeoloog-guh heeft de vondst van de eeuw gedaan. Maar het kleine skelet ligt nog in stukjes in het bottenzand.",
        stappen=[("Praat met de Archeoloog-guh", "Zoek de Fossiel-opgraving en praat met de Archeoloog-guh bij zijn tent.", opgraving),
                 ("Kwast vijf botten los", "Houd met het Guhkwastje rechtsklik ingedrukt op het bottenzand in de put, tot je vijf botten hebt.",
                  "De put van de Fossiel-opgraving"),
                 ("Zet het skelet in elkaar", "Klik met de botten op het rek onder het afdakje.", "Het rek bij de Fossiel-opgraving"),
                 ("Laat het de Archeoloog-guh zien", "Vertel de Archeoloog-guh dat de Tyrannoguhrus staat.", "De tent van de Archeoloog-guh")],
        klaar=("De Tyrannoguhrus Njex staat! In het bottenzand ligt elke dag weer iets nieuws om los te kwasten.", opgraving),
        kort={"0": "Praat met de Archeoloog-guh", "1": "Kwast vijf botten los uit het bottenzand", "2": "Zet de botten op het rek",
              "3": "Vertel het de Archeoloog-guh"})
    mijn = "De Zoutkristalmijn in de Guhbarbecuether"
    verhaal_motor.verhaallijn(
        h, "mijnwerker", "Zout op de boterham",
        "Het karrenspoor van de Zoutkristalmijn ligt vol puin, en de Mijnwerker-guh heeft al dagen geen zout op zijn boterham.",
        stappen=[("Praat met de Mijnwerker-guh", "Zoek de Zoutkristalmijn en praat met de Mijnwerker-guh bij zijn schuurtje.", mijn),
                 ("Ruim het karrenspoor op", "Hak vijf brokken puin van het spoor in de gangen van de mijn.", "De gangen van de Zoutkristalmijn"),
                 ("Vind de kristalader", "Volg het spoor tot in de grot en hak met een houweel in de roze kristalader.",
                  "De grot aan het eind van het spoor"),
                 ("Zout voor de Mijnwerker-guh", "Breng drie zoutkristallen naar de Mijnwerker-guh.", "Het schuurtje bij de mijn")],
        klaar=("De kristalader is van jou: hij groeit altijd weer aan. Met zoutkristal maak je de slimme machines van de Guh-technologie.", mijn),
        kort={"0": "Praat met de Mijnwerker-guh", "1": "Hak vijf brokken puin van het spoor", "2": "Hak in de kristalader in de grot",
              "3": "Breng drie zoutkristallen naar de Mijnwerker-guh"})


def advancements(h):
    bbq2.zichtbaar(h, "barbecuether", "fossiel_mijn_archeoloog", "binnen", "guhs:fossielmijn_fossielbeeldje", "goal", "Tyrannoguhrus Njex",
                   "Kwast de botten los en zet het skelet in elkaar bij de Archeoloog-guh")
    bbq2.zichtbaar(h, "barbecuether", "fossiel_mijn_mijnwerker", "binnen", "guhs:fossielmijn_zoutkristalhouweel", "goal", "Zout op de boterham",
                   "Ruim het karrenspoor op en vind de kristalader van de Mijnwerker-guh")
    bbq2.zichtbaar(h, "barbecuether", "fossiel_mijn_zoutkristal", "binnen", "guhs:zoutkristal", "task", "Een korreltje zout",
                   "Vind zoutkristal: in de kristalader van de Zoutkristalmijn of in zoutkristalerts",
                   criteria={"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "guhs:zoutkristal"}]}}})
    for name in ("fossiel_mijn_dagvondst", "fossiel_mijn_beeldje"):
        bbq2.verborgen(h, name)


# =====================================================================================================================
# build, self-check
# =====================================================================================================================
def selfcheck(h):
    A, D = h.A, h.D
    missing = []
    for b in BLOKKEN:
        for path in (f"{A}/blockstates/{b}.json", f"{A}/models/item/{b}.json", f"{D}/loot_table/blocks/{b}.json"):
            if not os.path.exists(path):
                missing.append(path)
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for i in ITEMS:
        if not os.path.exists(f"{A}/models/item/{i}.json") or not os.path.exists(f"{A}/textures/item/{i}.png"):
            missing.append(f"item {i}")
        if f"item.guhs.{i}" not in h.NL:
            missing.append(f"lang item.guhs.{i}")
    for f in sorted(os.listdir(f"{A}/models/block")):
        if f.startswith("fossielmijn_"):
            model = json.load(open(f"{A}/models/block/{f}", encoding="utf-8"))
            for t in model.get("textures", {}).values():
                if t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                    missing.append(f"texture {t} ({f})")
            for e in model.get("elements", []):
                if min(e["from"]) < -16 or max(e["to"]) > 32:
                    missing.append(f"{f}: an element outside -16..32")
    for naam in (bouw.OPGRAVING, bouw.MIJN):
        pool = json.load(open(f"{D}/worldgen/template_pool/{naam}/start.json", encoding="utf-8"))
        if any(e["element"].get("element_type") != "guhs:grond_single_pool_element" for e in pool["elements"]):
            missing.append(f"{naam}: its start pool has no ground_level_delta")
        for path in (f"{D}/structure/{naam}.nbt", f"{D}/worldgen/structure/{naam}.json", f"{D}/worldgen/structure_set/{naam}.json",
                     f"{D}/worldgen/structure_set/{naam}_gegarandeerd.json"):
            if not os.path.exists(path):
                missing.append(path)
    for kind in ("archeoloogguh", "mijnwerkerguh"):
        if not os.path.exists(f"{A}/geckolib/models/entity/guh_npc_{kind}.geo.json"):
            missing.append(f"model of {kind}")
    if missing:
        raise SystemExit("fossiel_mijn self-check failed:\n  " + "\n  ".join(missing))


def build(h):
    textures(h)
    modellen(h)
    data(h)
    erts(h)
    structuren(h)
    npcs(h)
    teksten(h)
    advancements(h)
    selfcheck(h)


# =====================================================================================================================
# FTB quests (chapter guhs_barbecuether, two sections)
# =====================================================================================================================
def ftb(fq):
    q, item, adv, structure = fq.q, fq.item, fq.adv, fq.structure
    q("fossiel_mijn_opgraving_vind", "Botten in de as",
      "Ergens in de Guhbarbecuether graaft de &dArcheoloog-guh&r het skelet van een reuzenguh op: de &6Fossiel-opgraving&r. "
      "Het superkompas (Barbecue > Fossiel-opgraving) wijst de weg.",
      "minecraft:bone_block", [structure("fossiel_opgraving")], rewards=(("guhs:kaas_knabbels", 12),), deps=["bbq_dimensie"], shape="hexagon", xp=100)
    wereld.ftb_questlijn(fq, "fossiel_mijn", "archeoloog", [
        ("De vondst van de eeuw", "Praat met de &dArcheoloog-guh&r bij zijn tent. Je krijgt een &6Guhkwastje&r van hem.", "guhs:fossielmijn_kwastje"),
        ("Kwasten maar!", "Houd met het Guhkwastje rechtsklik ingedrukt op het &6bottenzand&r in de put (daar steekt een puntje bot uit de as). "
                          "Elk plekje geeft jou één bot; vijf botten heb je nodig. Iedereen vindt zijn eigen botten, ook als er al iemand kwastte.",
         "guhs:fossielmijn_bot_ribben"),
        ("Bot voor bot", "Klik met de botten op het &6rek&r onder het afdakje. Alleen jij ziet jouw skelet groeien.", "guhs:fossielmijn_bot_schedel"),
        ("Tyrannoguhrus Njex", "Vertel de Archeoloog-guh dat hij staat! Je krijgt een &6Tyrannoguhrus-beeldje&r en mag het kwastje houden.",
         "guhs:fossielmijn_fossielbeeldje")],
        na=["fossiel_mijn_opgraving_vind"], eind=(("guhs:kaas_knabbels", 16),))
    q("fossiel_mijn_dagvondst", "Elke dag een botje", "Na het skelet ligt er in elk plekje bottenzand elke dag weer iets voor jou: een botje, wat knabbels, "
      "soms een zoutkristal. Kwast er eentje los!", "guhs:fossielmijn_kwastje", [adv("fossiel_mijn_dagvondst")],
      rewards=(("minecraft:bone_meal", 8),), deps=["fossiel_mijn_archeoloog_4"], shape="circle")
    q("fossiel_mijn_beeldje", "Njex op de kast", "Zet je Tyrannoguhrus-beeldje ergens neer waar iedereen het ziet.", "guhs:fossielmijn_fossielbeeldje",
      [adv("fossiel_mijn_beeldje")], deps=["fossiel_mijn_archeoloog_4"], shape="diamond")

    q("fossiel_mijn_mijn_vind", "Een berg van zout", "Een witte berg vol kristallen op de bodem van een grot: de &6Zoutkristalmijn&r. "
      "Het superkompas (Barbecue > Zoutkristalmijn) wijst de weg.",
      "guhs:fossielmijn_zoutkristalblok", [structure("zoutkristalmijn")], rewards=(("guhs:kaas_knabbels", 12),), deps=["bbq_dimensie"],
      shape="hexagon", xp=100)
    wereld.ftb_questlijn(fq, "fossiel_mijn", "mijnwerker", [
        ("Wat een puinhoop", "Praat met de &dMijnwerker-guh&r bij zijn schuurtje voor de mijn.", "minecraft:rail"),
        ("Het spoor vrij", "Hak vijf brokken &6puin&r van het karrenspoor in de gangen. (Het puin valt na een minuutje weer terug voor de volgende "
                           "speler: jouw vijf tellen gewoon.)", "guhs:fossielmijn_puin"),
        ("De kristalader", "Volg het spoor tot in de grot en hak met een houweel in de roze &6kristalader&r.", "guhs:fossielmijn_zoutader"),
        ("Zout op de boterham", "Breng drie zoutkristallen naar de Mijnwerker-guh. Je krijgt zijn &6Zoutkristalhouweel&r: daarmee geeft de ader "
                                "twee keer zoveel.", "guhs:fossielmijn_zoutkristalhouweel")],
        na=["fossiel_mijn_mijn_vind"], eind=(("guhs:kaas_knabbels", 16),))
    q("fossiel_mijn_zout", "Een zak zout", "De kristalader is van iedereen een beetje: elke speler heeft er een eigen voorraad in, die vanzelf weer "
      "aangroeit. Verzamel 16 &6zoutkristallen&r: daarmee begint de Guh-technologie met zout.",
      "guhs:zoutkristal", [item("guhs:zoutkristal", 16)], rewards=(("guhs:kaas_knabbels", 12),), deps=["fossiel_mijn_mijnwerker_3"], shape="rsquare", xp=100)
    q("fossiel_mijn_blok", "Zoute bouwstenen", "Vier zoutkristallen maken een &6zoutkristalblok&r: het geeft zacht licht. Maak er een paar.",
      "guhs:fossielmijn_zoutkristalblok", [item("guhs:fossielmijn_zoutkristalblok", 4)], deps=["fossiel_mijn_zout"], shape="diamond")
