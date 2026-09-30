"""
3.0 (Guhverhalen), slice balto: Baltoguh en Nomguh (DESIGN_30 §2) - Java: feature/balto.

  build(h)    the textures (balto_tex), the models of the characters and the blocks (balto_modellen), the blocks' blockstates,
              loot, recipes and tags, the Sneeuwguhtoendra's content (balto_wereld: sneeuwguhspar groves, snow drifts,
              boulders, frosty sprigs, its surface), the sounds, the texts (balto_tekst), the advancements (tab
              guhs:verhalen/), Nomguh (balto_bouw + balto_gebouwen: the template, its regio structure, and
              assets/guhs/nomguh/route.json for balto-slee), the Baltoguh's fur (the wolf look painted over the grey of
              make_guh_variants, and its sleeping eyes), the game test templates. Self-checks raise SystemExit.
  ftb(fq)     the section "Baltoguh en Nomguh" of the chapter guhs_verhalen.
  BONES / variants / clothes / icons / CLOTHES: the Baltoguh's wolf ears and bushy tail (variant bones balto_*), and the four
              outfits of the questline (rood sjaaltje, wolfsoortjes, sneeuwmuts met pompon, wantjes; source "nomguh").
The sounds are made by balto_geluid.py (run separately; the OGGs are committed).
"""
import json
import os

import numpy as np
from PIL import Image

from features import balto_bouw as bouw
from features import balto_gebouwen as geb
from features import balto_modellen as modellen
from features import balto_tekst as tekst
from features import balto_tex as tex
from features import balto_wereld as wereld
from features import barbecuether
from features import spelen
from features import verhaal
from features import verhaal_wereld

FTB_SECTION = "Baltoguh en Nomguh"
BEELDJE_KLAAR = True          # (verhaal.py: the Verhalen tab's icon is the beeldje)

# =====================================================================================================================
# the Baltoguh's wolf ears and tail (variant bones, prefix balto: only the Baltoguh shows them) and the outfits' bones
# =====================================================================================================================
_H = [0, 6, -2]


def _oor(sw_buiten, sw_binnen):
    """A big pointy wolf ear round the left guh ear (the right one is mirrored by spelen.oren / _spiegel)."""
    return [([4.2, 10.5, -6.8], [7.8, 6, 2.6], 0), ([5.2, 16.5, -6.6], [5.8, 3, 2.2], 0), ([6.2, 19.5, -6.4], [3.8, 2.5, 1.8], 0),
            ([7.0, 22, -6.2], [2.2, 1.8, 1.4], 0)], [([5.6, 12, -7.05], [5.0, 7, 0.3], 0)]


def _spiegel(cubes):
    return [([-(o[0] + s[0]), o[1], o[2]], list(s), i) for o, s, i in cubes]


_BUITEN, _BINNEN = _oor(None, None)
_EL, _ER = spelen.EAR_LEFT_PIVOT, spelen.EAR_RIGHT_PIVOT
BONES = {
    # the Baltoguh (GuhVariant BALTOGUH shows bones starting with "balto"): two swatches, balto_vacht and balto_licht
    "balto_oor_links": ("ear_left", _EL, "balto_vacht", _BUITEN),
    "balto_oor_rechts": ("ear_right", _ER, "balto_vacht", _spiegel(_BUITEN)),
    "balto_oor_links_binnen": ("ear_left", _EL, "balto_licht", _BINNEN),
    "balto_oor_rechts_binnen": ("ear_right", _ER, "balto_licht", _spiegel(_BINNEN)),
    "balto_staart": ("tail", [0, 1.5, 12], "balto_vacht", [([-1.7, 2.3, 12.4], [3.4, 2.6, 3.6], 0), ([-2.1, 2.1, 15.8], [4.2, 3.0, 3.0], 0)]),
    "balto_staart_punt": ("tail", [0, 1.5, 12], "balto_licht", [([-1.6, 2.4, 18.6], [3.2, 2.4, 2.2], 0)]),
    # the outfits (feature pool): the red scarf's knot and ends, the snow hat with its pompon and braids, the mittens
    "outfit_balto_sjaal": ("body", [0, 6, 6], "balto_sjaal", [([-7.6, 7.0, -2.6], [2.4, 2.4, 2.0], 0), ([-8.2, 2.6, -2.4], [1.2, 4.4, 1.6], 0),
                                                              ([-7.0, 3.4, -3.0], [1.2, 3.6, 1.4], 0)]),
    "outfit_balto_muts": ("head", _H, "balto_muts", [([-4.6, 14.4, -9.6], [9.2, 1.8, 8.2], 0.1), ([-4.2, 16.0, -9.2], [8.4, 2.8, 7.4], 0),
                                                     ([-3.4, 18.8, -8.4], [6.8, 1.2, 5.8], 0)]),
    "outfit_balto_muts_pompon": ("head", _H, "balto_pompon", [([-1.6, 19.6, -7.1], [3.2, 3.2, 3.2], 0.15)]),
    "outfit_balto_muts_touw": ("head", _H, "balto_pompon", [([-5.0, 9.6, -6.4], [0.8, 5.0, 0.8], 0), ([4.2, 9.6, -6.4], [0.8, 5.0, 0.8], 0),
                                                           ([-5.2, 8.4, -6.6], [1.2, 1.4, 1.2], 0), ([4.0, 8.4, -6.6], [1.2, 1.4, 1.2], 0)]),
    "outfit_balto_want_links": ("leg_front_left", [2, 1, 0], "balto_want", [([4.0, -0.2, -2.8], [4.0, 2.4, 5.0], 0.1),
                                                                            ([7.6, 0.4, -2.2], [1.0, 1.4, 1.6], 0)]),
    "outfit_balto_want_rechts": ("leg_front_right", [-6, 1, 0], "balto_want", [([-8.0, -0.2, -2.8], [4.0, 2.4, 5.0], 0.1),
                                                                               ([-8.6, 0.4, -2.2], [1.0, 1.4, 1.6], 0)]),
    "outfit_balto_want_touw": ("body", [0, 6, 6], "balto_touw", [([-6.6, 1.6, -3.0], [13.2, 0.6, 0.6], 0), ([5.8, 1.6, -3.0], [0.6, 3.0, 0.6], 0),
                                                                ([-6.4, 1.6, -3.0], [0.6, 3.0, 0.6], 0)]),
}
# the wolf ears as an OREN outfit (every guh may wear them): grey, a cream inside
# (a little inflated: on a Baltoguh they cover his own wolf ears instead of flickering with them)
BONES.update(spelen.oren("outfit_oren_balto", "balto_wolfsoor", cubes=[(o, s, 0.2) for o, s, i in _BUITEN + _BINNEN]))
CLOTHES = ["balto_sjaaltje", "balto_wolfsoortjes", "balto_sneeuwmuts", "balto_wantjes"]


def variants(rng, v):
    """The Baltoguh: grey fur (the wolf look, the light belly and the dark saddle are painted by build: fur())."""
    return {"baltoguh": ((150, 152, 162), {"balto_vacht": lambda: v.fabric((138, 140, 152), rng, 14),
                                          "balto_licht": lambda: v.fabric((238, 234, 228), rng, 8)})}


def clothes(rng, v):
    def sjaal():
        a = v.fabric((214, 44, 52), rng, 10)
        for y in range(0, 32, 4):
            a[y, :] = a[y, :] * 0.86
        a[26:29, :] = (250, 250, 250)
        return a

    def muts():
        a = v.fabric((150, 206, 238), rng, 8)
        for y in (3, 4, 11, 12):
            a[y, :] = (250, 250, 252)
        for x in range(2, 32, 7):                     # little snowflakes in the band
            a[7, x:x + 3] = (250, 250, 252)
            a[6:9, x + 1] = (250, 250, 252)
        return a

    def pompon():
        return v.fabric((252, 252, 255), rng, 14)

    def want():
        a = v.fabric((220, 50, 60), rng, 10)
        a[24:28, :] = (250, 250, 250)
        for x in range(3, 32, 8):
            a[10:13, x:x + 2] = (250, 250, 250)
        return a

    def touw():
        return v.fabric((240, 236, 226), rng, 6)

    def oor():
        return v.fabric((140, 142, 154), rng, 14)

    return {
        "balto_sjaaltje": {"scarf": sjaal, "balto_sjaal": sjaal},
        "balto_wolfsoortjes": {"balto_wolfsoor": oor},
        "balto_sneeuwmuts": {"balto_muts": muts, "balto_pompon": pompon},
        "balto_wantjes": {"balto_want": want, "balto_touw": touw},
    }


def icons(ic):
    sjaal = ["..aaaaaaaaaaaa..", ".arrrrrrrrrrrra.", ".arrrrrrrrrrrra.", "..aaaaaaarrra...", "........arrra...",
             ".......arrra....", ".......awwwa....", "........arrra...", "........awwwa...", "........aaaaa..."]
    oren = [".a..........a...", ".ga........ag...", ".gga......agg...", ".gcga....agcg...", ".gccga..agccg...",
            ".gccgaaaagccg...", "..aaa.a..a.aaa..", "......aaaa......"]
    muts = ["......ww......", ".....wwww.....", "......ww......", "....aaaaaa....", "...abbbbbba...", "..abwbwbwbba..",
            "..awwwwwwwwa..", "..abbbbbbbba..", "..aaaaaaaaaa..", "..w........w..", "..w........w.."]
    want = ["....aaa..aaa....", "...arrraarrra...", "...arrraarrra...", "...arwraarwra...", "...arrraarrra...",
            "...awwwaawwwa...", "....aaa..aaa....", "....t......t....", ".....tttttt....."]
    return {
        "balto_sjaaltje": ic.icon(ic.pad(sjaal), {"a": (130, 20, 30), "r": (214, 44, 52), "w": (250, 250, 250)}),
        "balto_wolfsoortjes": ic.icon(ic.pad(oren), {"a": (70, 70, 84), "g": (140, 142, 154), "c": (238, 234, 228)}),
        "balto_sneeuwmuts": ic.icon(ic.pad(muts), {"a": (70, 120, 160), "b": (150, 206, 238), "w": (252, 252, 255)}),
        "balto_wantjes": ic.icon(ic.pad(want), {"a": (130, 20, 30), "r": (220, 50, 60), "w": (250, 250, 250), "t": (200, 190, 170)}),
    }


# =====================================================================================================================
# blocks: blockstates, models, loot, recipes, tags
# =====================================================================================================================
def blocks(h):
    A, D, w = h.A, h.D, h.w
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    # the track snow
    w(f"{A}/models/block/nomguh_sneeuwspoor.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "guhs:block/nomguh_sneeuwspoor_top", "bottom": "minecraft:block/snow", "side": "guhs:block/nomguh_sneeuwspoor_side"}})
    w(f"{A}/blockstates/nomguh_sneeuwspoor.json", {"variants": {"": [{"model": "guhs:block/nomguh_sneeuwspoor", **({"y": r} if r else {})}
                                                                     for r in (0, 90, 180, 270)]}})
    w(f"{A}/models/item/nomguh_sneeuwspoor.json", {"parent": "guhs:block/nomguh_sneeuwspoor"})
    # the snowy roof tiles (+ stairs, slab)
    t = {"top": "guhs:block/nomguh_sneeuwdak_top", "bottom": "minecraft:block/spruce_planks", "side": "guhs:block/nomguh_sneeuwdak_side"}
    w(f"{A}/models/block/nomguh_sneeuwdak.json", {"parent": "minecraft:block/cube_bottom_top", "textures": t})
    w(f"{A}/blockstates/nomguh_sneeuwdak.json", {"variants": {"": {"model": "guhs:block/nomguh_sneeuwdak"}}})
    w(f"{A}/models/item/nomguh_sneeuwdak.json", {"parent": "guhs:block/nomguh_sneeuwdak"})
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        w(f"{A}/models/block/nomguh_sneeuwdak_trap{suffix}.json", {"parent": f"minecraft:block/{parent}", "textures": t})
    w(f"{A}/blockstates/nomguh_sneeuwdak_trap.json", {"variants": barbecuether.stair_states("guhs:block/nomguh_sneeuwdak_trap")})
    w(f"{A}/models/item/nomguh_sneeuwdak_trap.json", {"parent": "guhs:block/nomguh_sneeuwdak_trap"})
    w(f"{A}/models/block/nomguh_sneeuwdak_plaat.json", {"parent": "minecraft:block/slab", "textures": t})
    w(f"{A}/models/block/nomguh_sneeuwdak_plaat_top.json", {"parent": "minecraft:block/slab_top", "textures": t})
    w(f"{A}/blockstates/nomguh_sneeuwdak_plaat.json", {"variants": {"type=bottom": {"model": "guhs:block/nomguh_sneeuwdak_plaat"},
                                                                    "type=top": {"model": "guhs:block/nomguh_sneeuwdak_plaat_top"},
                                                                    "type=double": {"model": "guhs:block/nomguh_sneeuwdak"}}})
    w(f"{A}/models/item/nomguh_sneeuwdak_plaat.json", {"parent": "guhs:block/nomguh_sneeuwdak_plaat"})
    # the sneeuwguhspar
    w(f"{A}/models/block/sneeuwguhspar_stam.json", {"parent": "minecraft:block/cube_column", "textures": {
        "end": "guhs:block/sneeuwguhspar_stam_top", "side": "guhs:block/sneeuwguhspar_stam"}})
    w(f"{A}/models/block/sneeuwguhspar_stam_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": {
        "end": "guhs:block/sneeuwguhspar_stam_top", "side": "guhs:block/sneeuwguhspar_stam"}})
    w(f"{A}/blockstates/sneeuwguhspar_stam.json", {"variants": {
        "axis=y": {"model": "guhs:block/sneeuwguhspar_stam"},
        "axis=z": {"model": "guhs:block/sneeuwguhspar_stam_horizontal", "x": 90},
        "axis=x": {"model": "guhs:block/sneeuwguhspar_stam_horizontal", "x": 90, "y": 90}}})
    w(f"{A}/models/item/sneeuwguhspar_stam.json", {"parent": "guhs:block/sneeuwguhspar_stam"})
    w(f"{A}/models/block/sneeuwguhspar_gezicht.json", {"parent": "minecraft:block/orientable", "textures": {
        "front": "guhs:block/sneeuwguhspar_gezicht", "side": "guhs:block/sneeuwguhspar_stam", "top": "guhs:block/sneeuwguhspar_stam_top"}})
    w(f"{A}/blockstates/sneeuwguhspar_gezicht.json", {"variants": {f"facing={f}": {"model": "guhs:block/sneeuwguhspar_gezicht", **({"y": r} if r else {})}
                                                                   for f, r in rot.items()}})
    w(f"{A}/models/item/sneeuwguhspar_gezicht.json", {"parent": "guhs:block/sneeuwguhspar_gezicht"})
    w(f"{A}/models/block/sneeuwguhspar_naalden.json", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:cutout_mipped",
                                                        "textures": {"all": "guhs:block/sneeuwguhspar_naalden"}})
    w(f"{A}/models/block/sneeuwguhspar_naalden_sneeuw.json", {"parent": "minecraft:block/cube_bottom_top", "render_type": "minecraft:cutout_mipped",
                                                               "textures": {"top": "guhs:block/sneeuwguhspar_naalden_sneeuw_top",
                                                                            "side": "guhs:block/sneeuwguhspar_naalden_sneeuw",
                                                                            "bottom": "guhs:block/sneeuwguhspar_naalden"}})
    w(f"{A}/blockstates/sneeuwguhspar_naalden.json", {"variants": {"sneeuw=false": {"model": "guhs:block/sneeuwguhspar_naalden"},
                                                                   "sneeuw=true": {"model": "guhs:block/sneeuwguhspar_naalden_sneeuw"}}})
    w(f"{A}/models/item/sneeuwguhspar_naalden.json", {"parent": "guhs:block/sneeuwguhspar_naalden_sneeuw"})
    w(f"{A}/models/block/sneeuwguhspar_zaailing.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                         "textures": {"cross": "guhs:block/sneeuwguhspar_zaailing"}})
    w(f"{A}/blockstates/sneeuwguhspar_zaailing.json", {"variants": {"stage=0": {"model": "guhs:block/sneeuwguhspar_zaailing"},
                                                                    "stage=1": {"model": "guhs:block/sneeuwguhspar_zaailing"}}})
    h.item_model("sneeuwguhspar_zaailing", "guhs:block/sneeuwguhspar_zaailing")
    # loot
    for b in ("baltoguh_beeldje", "nomguh_routepaal", "nomguh_sneeuwspoor", "nomguh_sneeuwdak", "nomguh_sneeuwdak_trap", "nomguh_medicijnkist",
              "sneeuwguhspar_stam", "sneeuwguhspar_gezicht", "sneeuwguhspar_zaailing"):
        h.self_drop(b)
    w(f"{D}/loot_table/blocks/nomguh_sneeuwdak_plaat.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "guhs:nomguh_sneeuwdak_plaat", "functions": [
            {"function": "minecraft:set_count", "count": 2, "conditions": [{"condition": "minecraft:block_state_property",
                                                                             "block": "guhs:nomguh_sneeuwdak_plaat", "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}]})
    shears = {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}}
    silk_or_shears = {"condition": "minecraft:any_of", "terms": [shears, {"condition": "minecraft:match_tool", "predicate": {
        "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]}
    w(f"{D}/loot_table/blocks/sneeuwguhspar_naalden.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "guhs:sneeuwguhspar_naalden", "conditions": [silk_or_shears]},
            {"type": "minecraft:item", "name": "guhs:sneeuwguhspar_zaailing", "conditions": [
                {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune", "chances": [0.05, 0.0625, 0.083, 0.1]}]}]}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:stick", "conditions": [
            {"condition": "minecraft:inverted", "term": silk_or_shears}, {"condition": "minecraft:random_chance", "chance": 0.02}],
            "functions": h.count_fn(1, 2)}]}]})
    # recipes
    h.shaped("nomguh_routepaal", ["R", "W", "S"], {"R": "minecraft:glowstone_dust", "W": "minecraft:red_wool", "S": "minecraft:stick"},
             "guhs:nomguh_routepaal", 4)
    h.shaped("nomguh_sneeuwdak", ["SS", "TT"], {"S": "minecraft:snow_block", "T": "#minecraft:terracotta"}, "guhs:nomguh_sneeuwdak", 4)
    h.shaped("nomguh_sneeuwdak_trap", ["D  ", "DD ", "DDD"], {"D": "guhs:nomguh_sneeuwdak"}, "guhs:nomguh_sneeuwdak_trap", 4)
    h.shaped("nomguh_sneeuwdak_plaat", ["DDD"], {"D": "guhs:nomguh_sneeuwdak"}, "guhs:nomguh_sneeuwdak_plaat", 6)
    h.shapeless("nomguh_sneeuwspoor", ["minecraft:snow_block", "minecraft:snow_block", "minecraft:gravel"], "guhs:nomguh_sneeuwspoor", 2)
    h.shapeless("sneeuwguhspar_planken", ["guhs:sneeuwguhspar_stam"], "minecraft:spruce_planks", 4)
    h.shapeless("sneeuwguhspar_planken_gezicht", ["guhs:sneeuwguhspar_gezicht"], "minecraft:spruce_planks", 4)
    # tags
    add = h.add_tag
    add("minecraft/tags/block/mineable/shovel", ["guhs:nomguh_sneeuwspoor"])
    add("minecraft/tags/block/mineable/axe", ["guhs:sneeuwguhspar_stam", "guhs:sneeuwguhspar_gezicht", "guhs:nomguh_sneeuwdak",
                                              "guhs:nomguh_sneeuwdak_trap", "guhs:nomguh_sneeuwdak_plaat", "guhs:nomguh_routepaal",
                                              "guhs:nomguh_medicijnkist"])
    add("minecraft/tags/block/mineable/pickaxe", ["guhs:baltoguh_beeldje"])
    add("minecraft/tags/block/mineable/hoe", ["guhs:sneeuwguhspar_naalden"])
    add("minecraft/tags/block/stairs", ["guhs:nomguh_sneeuwdak_trap"])
    add("minecraft/tags/block/slabs", ["guhs:nomguh_sneeuwdak_plaat"])
    add("minecraft/tags/item/stairs", ["guhs:nomguh_sneeuwdak_trap"])
    add("minecraft/tags/item/slabs", ["guhs:nomguh_sneeuwdak_plaat"])
    for kind in ("block", "item"):
        add(f"minecraft/tags/{kind}/logs_that_burn", ["guhs:sneeuwguhspar_stam", "guhs:sneeuwguhspar_gezicht"])
        add(f"minecraft/tags/{kind}/leaves", ["guhs:sneeuwguhspar_naalden"])
        add(f"minecraft/tags/{kind}/saplings", ["guhs:sneeuwguhspar_zaailing"])


def sounds(h):
    entries = {
        "balto.huil": [{"name": "guhs:balto/huil1"}, {"name": "guhs:balto/huil2"}],
        "balto.gak": [{"name": "guhs:balto/gak1"}, {"name": "guhs:balto/gak2"}, {"name": "guhs:balto/gak3"}],
        "balto.belletjes": [{"name": "guhs:balto/belletjes"}],
        "balto.hatsjoe": [{"name": "guhs:balto/hatsjoe1"}, {"name": "guhs:balto/hatsjoe2"}],
        "balto.snuffel": [{"name": "guhs:balto/snuffel"}],
        "balto.wind": [{"name": "guhs:balto/wind1"}, {"name": "guhs:balto/wind2"}],
    }

    def patch(d):
        for event, snd in entries.items():
            d[event] = {"sounds": snd, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def advancements(h):
    for name, parent, icon, frame, titel, desc, crit in tekst.ADVANCEMENTS:
        verhaal.zichtbaar(h, "verhalen", name, parent, icon, frame, titel, desc, criteria=crit)
    for name, *_ in tekst.ADVANCEMENTS:
        h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for page in ("baltoguh", "boris", "steele_mika", "muk", "luk", "rosy", "witte_wolfguh"):
        h.w(f"{h.D}/advancement/quest/seen_{page}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


# =====================================================================================================================
# the Baltoguh's fur
# =====================================================================================================================
def fur(h):
    """The wolf look on guh_baltoguh.png (made grey by make_guh_variants, with the ear and tail swatches): a light belly,
    chest, chin and paws, a darker saddle on the back, a little darker ear tips; then its sleeping eyes again."""
    from features import guhpolder_tex as gt
    geo = json.load(open(os.path.join(h.A, "geo", "entity", "guh.geo.json"), encoding="utf-8"))["minecraft:geometry"][0]
    base = np.asarray(Image.open(os.path.join(h.TEX, "entity", "guh.png")).convert("RGBA")).astype(np.int32)
    doel = Image.open(os.path.join(h.TEX, "entity", "guh_baltoguh.png")).convert("RGBA")
    a = np.asarray(doel).astype(np.int32).copy()
    diff = base[..., :3] - np.array(gt.FUR)
    furmask = (np.abs(diff).sum(-1) < 40) & (base[..., 3] > 0)

    def kleur(r, colour, rows=None):
        x0, y0, x1, y1 = r
        if rows:
            hh = y1 - y0
            y0, y1 = y0 + int(round(rows[0] * hh)), y0 + int(round(rows[1] * hh))
        m = np.zeros(furmask.shape, bool)
        m[y0:y1, x0:x1] = True
        m &= furmask
        a[..., :3] = np.where(m[..., None], np.clip(np.array(colour) + diff, 0, 255), a[..., :3])

    licht, zadel = (236, 232, 224), (112, 114, 128)
    for cube in gt._faces(geo, "body"):
        kleur(cube["down"], licht)
        kleur(cube["north"], licht, rows=(0.35, 1.0))
        kleur(cube["up"], zadel)
        for side in ("east", "west"):
            kleur(cube[side], licht, rows=(0.72, 1.0))
    for cube in gt._faces(geo, "head"):
        kleur(cube["down"], licht)
    for leg in ("leg_back_left", "leg_back_right", "leg_front_left", "leg_front_right"):
        for cube in gt._faces(geo, leg):
            for k, f in cube.items():
                if not k.startswith("_"):
                    kleur(f, licht, rows=(0.5, 1.0) if k != "down" else None)
    Image.fromarray(a.astype(np.uint8)).save(os.path.join(h.TEX, "entity", "guh_baltoguh.png"))
    import make_sleep_eyes
    make_sleep_eyes.sleepy("guh_baltoguh")


# =====================================================================================================================
# Nomguh
# =====================================================================================================================
def nomguh(h):
    s, info = bouw.build(h)
    route = bouw.route_json(info)
    rapport = bouw.check(s, info, route)
    s.save(bouw.NAME)
    h.w(f"{h.A}/nomguh/route.json", route)
    h.w(f"{h.D}/worldgen/template_pool/{bouw.NAME}/start.json", {"fallback": "minecraft:empty", "elements": [
        {"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:{bouw.NAME}",
                                  "projection": "rigid", "processors": "minecraft:empty"}}]})
    verhaal_wereld.regio_structuur(h, bouw.NAME, f"guhs:{bouw.NAME}/start", verhaal_wereld.TOENDRA_NOISE, "piek", 20300101,
                                   reach=bouw.REACH, voorrang=850, grond_y=bouw.G)
    for k, v in geb.TEKSTEN.items():
        h.lang(k, v, v)
    print(f"nomguh: route {rapport['lengte']} blocks, {rapport['punten']} points, {rapport['palen']} markers, "
          f"{rapport['rust']} rest points, ice bridge {rapport['brug']}, {rapport['blokken']} blocks")
    return route


# =====================================================================================================================
# test templates, self-check
# =====================================================================================================================
def test_templates(h):
    """balto_test_sneeuw: 20 x 6 x 12, the west half (x < 10) snow, the east half stone (the Baltoguh's snow speed)."""
    s = h.Structure((20, 6, 12))
    for x in range(20):
        for z in range(12):
            s.set(x, 0, z, "minecraft:snow_block" if x < 10 else "minecraft:stone")
    s.save("balto_test_sneeuw")


BLOKKEN = ["baltoguh_beeldje", "nomguh_routepaal", "nomguh_sneeuwspoor", "nomguh_sneeuwdak", "nomguh_sneeuwdak_trap", "nomguh_sneeuwdak_plaat",
           "nomguh_medicijnkist", "sneeuwguhspar_stam", "sneeuwguhspar_gezicht", "sneeuwguhspar_naalden", "sneeuwguhspar_zaailing"]


def selfcheck(h, route):
    problems = []
    for b in BLOKKEN:
        if not os.path.exists(f"{h.A}/blockstates/{b}.json"):
            problems.append(f"block {b}: no blockstate")
        if not os.path.exists(f"{h.A}/models/item/{b}.json"):
            problems.append(f"block {b}: no item model")
        if f"block.guhs.{b}" not in h.NL:
            problems.append(f"block {b}: no name")
    for kind in ("boris", "steele_mika", "muk", "luk", "rosy", "witte_wolfguh"):
        for p in (f"{h.A}/geo/entity/guh_npc_{kind}.geo.json", f"{h.TEX}/entity/npc_{kind}.png"):
            if not os.path.exists(p):
                problems.append(f"missing {p}")
    for snd in ("huil1", "gak1", "belletjes", "hatsjoe1", "snuffel", "wind1"):
        if not os.path.exists(f"{h.A}/sounds/balto/{snd}.ogg"):
            problems.append(f"sound balto/{snd}.ogg missing (python tools/features/balto_geluid.py)")
    for key, text in list(tekst.TEKSTEN.items()) + list(geb.TEKSTEN.items()):
        low = text.lower()
        if "te vads" in low or "hamster" in low:
            problems.append(f"lore: {key}")
    if len(route["punten"]) < 20 or route["lengte"] < 200:
        problems.append("the route is too short")
    if problems:
        raise SystemExit("balto self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    tex.textures(h)
    blocks(h)
    modellen.build(h)
    wereld.build(h)
    sounds(h)
    for k, v in tekst.TEKSTEN.items():
        h.lang(k, v, v)
    for c in CLOTHES:
        h.item_model(c)
    advancements(h)
    route = nomguh(h)
    fur(h)
    test_templates(h)
    selfcheck(h, route)


# =====================================================================================================================
# FTB: "Baltoguh en Nomguh" (chapter guhs_verhalen)
# =====================================================================================================================
def ftb(fq):
    q, adv, item = fq.q, fq.adv, fq.item
    q("balto_nomguh", "Nomguh", "Midden in de &fSneeuwguhtoendra&r ligt &fNomguh&r, een besneeuwd guhstadje met gekleurde huisjes, een "
      "ziekenhuisje en een sledehondenstal. Je &6superkompas&r (tab Verhalen) wijst de weg. Trek een warme sjaal aan, njeg!",
      "guhs:nomguh_routepaal", [adv("guhs:verhalen/balto_nomguh")], rewards=(("guhs:kaas_knabbels", 8),), shape="circle", xp=100)
    q("balto_ontmoet", "Een buitenbeentje", "Bij een oude boot op de bevroren baai zit &7Baltoguh&r: half guh, half wolf. De andere "
      "guhs vinden hem raar, en &cSteele-Mika&r lacht hem uit. Zeg eens hallo!", "minecraft:snowball", [adv("balto_ontmoet")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["balto_nomguh"], xp=100)
    q("balto_rosy", "Hatsjoe-njeg!", "In het &dziekenhuisje&r (het witte huis met het roze hart) liggen de guhbaby's te snotteren. "
      "&dRosy&r heeft de knabbelkuch, en het medicijn is op! De nieuwe medicijnkist ligt in de berghut op de Nomguhpieken...",
      "guhs:nomguh_medicijnkist", [adv("balto_rosy")], rewards=(("guhs:kaas_knabbels", 8),), deps=["balto_ontmoet"], xp=100)
    q("balto_boris", "Da, kleine guh... gak!", "&7Boris&r is een echte gans (geen guh!) met een bontmutsje. Hij weet de weg door de "
      "storm: volg de &crode paaltjes&r met de lampjes. En neem Baltoguh mee, zijn neus is de beste van Nomguh.", "minecraft:feather",
      [adv("balto_boris")], rewards=(("guhs:kaas_knabbels", 8),), deps=["balto_rosy"], xp=100)
    q("balto_tocht", "Door de sneeuwstorm", "Zeg tegen Baltoguh: op naar de berghut! Jij stuurt de sneeuwslee, Baltoguh en de "
      "guh-sledehondjes trekken. Door het Stormdal, over de IJsbrug, langs de Lawineberg, rust bij de vuurkorven... en pak de "
      "&dmedicijnkist&r in de berghut.", "guhs:nomguh_routepaal", [adv("balto_berghut")], rewards=(("guhs:kaas_knabbels", 16),),
      deps=["balto_boris"], xp=200)
    q("balto_wolf", "De witte wolf-guh", "Op de terugweg is de storm te erg. Baltoguh is de weg kwijt... Dan verschijnt er een witte "
      "wolf-guh, en hoor je Boris' woorden. Huil mee!", "minecraft:glow_berries", [adv("balto_wolf")], rewards=(("guhs:kaas_knabbels", 16),),
      deps=["balto_tocht"], xp=200)
    q("balto_held", "Maar heel misschien... een Baltoguh wel", "Kom op tijd terug in Nomguh (Steele-Mika wil natuurlijk de eer, "
      "njeh-heh) en breng de medicijnkist naar Rosy. Iedereen in Nomguh noemt jou dan de &6Held van Nomguh&r! Je krijgt een eigen "
      "&fsneeuwslee&r, het &6Baltoguh-beeldje&r en vier warme outfits.", "guhs:baltoguh_beeldje", [adv("balto_held")],
      rewards=(("guhs:kaas_knabbels", 32),), deps=["balto_wolf"], shape="gear", xp=500)
    q("balto_getemd", "Een eigen Baltoguh", "Na het verhaal wil Baltoguh met je mee naar huis (eenmalig, en alleen voor jou). In de "
      "sneeuw rent hij supervahoeg, ook als je op hem rijdt.", "minecraft:bone", [adv("verhaal_getemd_baltoguh")],
      rewards=(("guhs:kaas_knabbels", 16),), deps=["balto_held"], xp=200)
    q("balto_snuffel", "Snuf snuf, naar huis!", "Druk in het guhmenu van je Baltoguh op &bSnuffel!&r: hij ruikt de weg naar je huisje "
      "(of je bed) en er lichten gloeiende pootjes op in de sneeuw. Volg ze maar!", "minecraft:compass", [adv("balto_snuffel")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["balto_getemd"])
    q("balto_beeldje", "Een held op een sokkel", "Zet je &6Baltoguh-beeldje&r ergens mooi neer. Klik erop voor de beroemde woorden.",
      "guhs:baltoguh_beeldje", [adv("balto_beeldje")], rewards=(("guhs:kaas_knabbels", 8),), deps=["balto_held"])
    for key, titel, uitleg in (("sjaaltje", "Rood sjaaltje", "Het rode sjaaltje van Baltoguh, met een knoopje en twee wapperende "
                                                              "slippen."),
                               ("wolfsoortjes", "Wolfsoortjes", "Spitse grijze wolfsoortjes voor elke guh (op de guhoortjes)."),
                               ("sneeuwmuts", "Sneeuwmuts met pompon", "Een ijsblauwe muts met sneeuwvlokjes, een dikke pompon en "
                                                                       "twee vlechtjes."),
                               ("wantjes", "Wantjes aan een touwtje", "Rode wantjes voor de voorpootjes, aan een touwtje: nooit meer "
                                                                      "kwijt, njeg!")):
        q(f"balto_outfit_{key}", titel, uitleg + " Een beloning van de Held van Nomguh.", f"guhs:balto_{key}",
          [item(f"guhs:balto_{key}")], rewards=(("guhs:kaas_knabbels", 4),), deps=["balto_held"])
    q("balto_mukluk", "Muk en Luk", "Bij de iglo aan het ijsvijvertje wonen twee lieve ijsbeer-guhs: &bMuk&r en &aLuk&r. Zeg hallo tegen "
      "allebei (Luk zegt niks terug, maar knuffelt des te meer).", "minecraft:cod", [adv("balto_mukluk")],
      rewards=(("guhs:kaas_knabbels", 8),), deps=["balto_nomguh"])
    for page, naam, icoon in (("baltoguh", "Baltoguh", "minecraft:snowball"), ("boris", "Boris", "minecraft:feather"),
                              ("steele_mika", "Steele-Mika", "minecraft:gold_ingot"), ("muk", "Muk", "minecraft:cod"),
                              ("luk", "Luk", "minecraft:fishing_rod"), ("rosy", "Rosy", "guhs:nomguh_medicijnkist"),
                              ("witte_wolfguh", "De witte wolf-guh", "minecraft:glow_berries")):
        q(f"balto_dex_{page}", f"Guhdex: {naam}", f"Zet &d{naam}&r in je Guhdex (kom dichtbij" +
          (", alleen op het dieptepunt van de tocht)." if page == "witte_wolfguh" else ")."), icoon, [adv(f"seen_{page}")],
          rewards=(("guhs:kaas_knabbels", 4),), deps=["balto_nomguh"], shape="rsquare")
