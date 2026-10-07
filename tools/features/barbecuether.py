"""
De Guhbarbecuether (2.7.0, slice 1): a Nether parody, 1:8 from the Guhmensie.

  - the dimension guhs:barbecuether (dimension_type with ceiling, ultrawarm, scale 8; noise settings like the Nether's
    with a sea of kaasfrituursaus at y 31; own biome noises so it doesn't copy the world's Nether)
  - five biomes: houtskoolvlakte (nether wastes), satebos (crimson forest), worstenwoud (warped forest),
    asdal (soul sand valley) and rookdelta (basalt deltas), each with its own fog, particles, sounds and vegetation
  - the fluid kaasfrituursaus (textures, bucket), the block set, the grillkool portal and the Aanmaakblokje
  - the structure guhs:barbecueput (barbecuether_put.py): broken barbecues with a grillkool frame, the big one with
    the Grillguh, in the Guhmensie and in the Barbecuether
  - the Grillguh (texture, quest and shop lang, his chef outfit: GuhClothes GRILL_*), the Guhdex page,
    advancements (own tab), FTB quests, recipes, loot and tags

build(h) makes everything (h = make_v2). Java: src/main/java/nl/juiced/guhs/feature/barbecuether/.
"""
import json
import os
import zipfile

from PIL import Image

from . import barbecuether_put as put
from . import barbecuether_tex as tex

NAME = "barbecuether"
BIOMES = {  # id: (Dutch name, fog, sky, particle, sounds, mika weight)
    "houtskoolvlakte": ("Houtskoolvlakte", 0x4A1E0E, 0x6E3A1A,
                        {"options": {"type": "minecraft:dust", "color": [1.0, 0.45, 0.1], "scale": 0.6}, "probability": 0.005},
                        "nether_wastes", 12),
    "satebos": ("Satébos", 0x6A1608, 0x7A2A10, {"options": {"type": "minecraft:crimson_spore"}, "probability": 0.025},
                "crimson_forest", 6),
    "worstenwoud": ("Worstenwoud", 0x5C4A10, 0x6A5A1A,
                    {"options": {"type": "minecraft:dust", "color": [0.95, 0.78, 0.15], "scale": 0.8}, "probability": 0.012},
                    "warped_forest", 6),
    "asdal": ("Asdal", 0x3A3533, 0x4A4442, {"options": {"type": "minecraft:ash"}, "probability": 0.0625},
              "soul_sand_valley", 8),
    "rookdelta": ("Rookdelta", 0x6B6058, 0x7A6E64, {"options": {"type": "minecraft:white_ash"}, "probability": 0.118},
                  "basalt_deltas", 8),
}
# the multi_noise points (the Nether's): temperature, humidity, offset
BIOME_POINTS = {"houtskoolvlakte": (0.0, 0.0, 0.0), "asdal": (0.0, -0.5, 0.0), "satebos": (0.4, 0.0, 0.0),
                "worstenwoud": (0.0, 0.5, 0.375), "rookdelta": (-0.5, 0.0, 0.175)}

SIMPLE = ["houtskoolsteen", "houtskoolsteen_stenen", "gebarsten_houtskoolsteen_stenen", "gebeitelde_houtskoolsteen_stenen",
          "roosterijzer", "gepolijst_roosterijzer", "gloeikool", "as_aarde", "sate_vlees", "mosterd_blok", "uienlicht",
          "grillkool", "houtskoolsteen_kaasknabbelerts"]
PILLARS = ["roosterijzer_pilaar", "sate_stam", "worst_stam", "verkoold_guhbot"]
PLANTS = ["pindascheutjes", "mosterdscheutjes", "sate_zwammetje", "worst_zwammetje", "smeulkooltjes"]
ALL_BLOCKS = SIMPLE + PILLARS + PLANTS + ["houtskoolsteen_stenen_trap", "houtskoolsteen_stenen_plaat", "houtskoolsteen_stenen_muur",
                                         "houtskoolsteen_stenen_hek", "roosterijzer_tralies", "as_blok", "pindasaus_nylium",
                                         "mosterd_nylium", "pindasausplasje", "rookgat"]
ITEMS = ["aanmaakblokje", "gloeikoolgruis", "grillguh_recept", "kaasknabbelsate", "gegrilde_kaasknabbelsate", "guhbraadworst",
         "kaasfrituursaus_bucket"]
CLOTHES = ["grill_koksmuts", "grill_schort", "grill_halsdoek"]

# --- the Grillguh's chef outfit (guh clothes). The bones re-use swatches that are already on the guh texture (chef_hat,
# band, smul_apron, scarf): this piece's texture paints them in its own colours, so no texture space is used up. ---
_H = [0, 6, -2]
_BODY = [0, 6, 6]
BONES = {
    # a tall puffy chef's toque on a band (much taller than the plain chef's hat)
    "outfit_grill_koksmuts": ("head", _H, "chef_hat", [([-4, 16.8, -9.2], [8, 3.6, 6.4], 0), ([-4.8, 20.2, -10], [9.6, 2.6, 8], 0),
                                                      ([-3.8, 22.6, -9], [7.6, 1.2, 6], 0)]),
    "outfit_grill_koksmuts_band": ("head", _H, "band", [([-3.7, 15, -8.7], [7.4, 2, 5.4], 0)]),
    # a barbecue apron: a bib in front of the chest, a skirt over the belly, straps round the neck and the waist
    "outfit_grill_schort": ("body", _BODY, "smul_apron", [([-4.2, 2.2, -3.4], [8.4, 8.4, 0.8], 0), ([-6.6, 1.2, -3.2], [13.2, 3.4, 0.8], 0),
                                                         ([-4.6, 10.4, -3.1], [0.8, 0.8, 5], 0), ([3.8, 10.4, -3.1], [0.8, 0.8, 5], 0),
                                                         ([-6.8, 4.4, -2.8], [0.6, 0.8, 12.6], 0), ([6.2, 4.4, -2.8], [0.6, 0.8, 12.6], 0),
                                                         ([-1.2, 3.6, 9.6], [2.4, 2.4, 0.6], 0)]),
}


def clothes(rng, v):
    def toque():
        a = v.fabric((252, 252, 250), rng, 5)
        for y in range(0, a.shape[0], 6):                 # the pleats of a real toque
            a[y, :] = a[y, :] * 0.9
        return a

    def band():
        a = v.fabric((200, 40, 30), rng, 6)
        px = a.shape[0]
        for x in range(0, px, 8):                        # little orange flames on the band
            a[px // 2 - 2:px // 2 + 2, x + 2:x + 5] = (250, 150, 40)
            a[px // 2 - 4:px // 2 - 2, x + 3:x + 4] = (255, 220, 90)
        return a

    def apron():
        a = v.fabric((58, 52, 56), rng, 6)                # a charcoal-grey barbecue apron...
        px = a.shape[0]
        a[:, :2] = a[:, -2:] = (220, 90, 30)              # ...with an orange trim
        a[:2, :] = a[-2:, :] = (220, 90, 30)
        c = px // 2                                      # and a big guh face on the bib
        for y in range(px):
            for x in range(px):
                d = ((x - c) ** 2 + (y - c) ** 2) ** 0.5
                if d < px * 0.28:
                    a[y, x] = (238, 141, 173)
        for ex in (c - px // 8, c + px // 8):
            a[c - 2:c + 1, ex - 1:ex + 1] = (30, 20, 30)
        a[c + 3:c + 4, c - 2:c + 2] = (190, 60, 110)
        return a

    return {
        "grill_koksmuts": {"chef_hat": toque, "band": band},
        "grill_schort": {"smul_apron": apron},
        "grill_halsdoek": {"scarf": lambda: v.dots((210, 40, 30), (250, 250, 245), rng, every=5, size=1)},
    }


def icons(ic):
    toque = ["...aaa..aaa.....", "..abbbaabbba....", ".abbbbbbbbbba...", ".abbbcbbbcbba...", "..abbbbbbbba....", "...abbbbbba.....",
             "...abcbbcba.....", "...abbbbbba.....", "...rrrrrrrr.....", "...roroorrr.....", "...rrrrrrrr....."]
    apron = ["....a......a....", "....a......a....", "....abbbbbba....", "....abppppba....", "....apkppkpa....", "aaaaappmmppaaaaa",
             "...abbppppbba...", "..abbbbbbbbbba..", "..abbbbbbbbbba..", "..abbbbbbbbbba..", ".abbbbbbbbbbbba.", ".oooooooooooooo."]
    return {
        "grill_koksmuts": ic.icon(ic.pad(toque), {"a": (170, 160, 150), "b": (252, 252, 250), "c": (220, 216, 208),
                                                  "r": (200, 40, 30), "o": (250, 150, 40)}),
        "grill_schort": ic.icon(ic.pad(apron), {"a": (30, 26, 28), "b": (58, 52, 56), "p": (238, 141, 173), "k": (30, 20, 30),
                                                "m": (190, 60, 110), "o": (220, 90, 30)}),
        "grill_halsdoek": ic.shaped("scarf", (120, 20, 15), (210, 40, 30), (250, 250, 245)),
    }


# =====================================================================================================================
# blocks
# =====================================================================================================================
def blocks(h):
    A, D = h.A, h.D
    save = h.save
    # --- textures ---
    save(tex.houtskoolsteen(1), "block", "houtskoolsteen.png")
    br = tex.bricks(2)
    save(br, "block", "houtskoolsteen_stenen.png")
    save(tex.cracked(br, 3), "block", "gebarsten_houtskoolsteen_stenen.png")
    save(tex.guh_face_carved(4), "block", "gebeitelde_houtskoolsteen_stenen.png")
    save(tex.roosterijzer(5), "block", "roosterijzer.png")
    side, top = tex.roosterijzer_pilaar(6)
    save(side, "block", "roosterijzer_pilaar.png")
    save(top, "block", "roosterijzer_pilaar_top.png")
    save(tex.gepolijst_roosterijzer(7), "block", "gepolijst_roosterijzer.png")
    save(tex.tralies(8), "block", "roosterijzer_tralies.png")
    save(tex.gloeikool(9), "block", "gloeikool.png")
    save(tex.as_blok(10), "block", "as_blok.png")
    save(tex.as_aarde(11), "block", "as_aarde.png")
    side, top = tex.verkoold_guhbot(12)
    save(side, "block", "verkoold_guhbot.png")
    save(top, "block", "verkoold_guhbot_top.png")
    side, top = tex.sate_stam(13)
    save(side, "block", "sate_stam.png")
    save(top, "block", "sate_stam_top.png")
    save(tex.sate_vlees(14), "block", "sate_vlees.png")
    save(tex.mosterd_blok(15), "block", "mosterd_blok.png")
    side, top = tex.worst_stam(16)
    save(side, "block", "worst_stam.png")
    save(top, "block", "worst_stam_top.png")
    top, side = tex.nylium(tex.PEANUT, tex.PEANUT_LIGHT, tex.PEANUT_DARK, 21, (240, 206, 150))
    save(top, "block", "pindasaus_nylium.png")
    save(side, "block", "pindasaus_nylium_side.png")
    top, side = tex.nylium(tex.MUSTARD, tex.MUSTARD_LIGHT, tex.MUSTARD_DARK, 22, (140, 90, 20))
    save(top, "block", "mosterd_nylium.png")
    save(side, "block", "mosterd_nylium_side.png")
    save(tex.uienlicht(17), "block", "uienlicht.png")
    for i, p in enumerate(PLANTS):
        save(tex.plant(p, 30 + i), "block", f"{p}.png")
    save(tex.pindasausplasje(18), "block", "pindasausplasje.png")
    side, top = tex.rookgat(19)
    save(side, "block", "rookgat_side.png")
    save(top, "block", "rookgat_top.png")
    save(tex.grillkool(20), "block", "grillkool.png")
    overlay = Image.open(os.path.join(h.TEX, "block", "kaasknabbel_overlay.png")).convert("RGBA")
    save(tex.ore_overlay(tex.houtskoolsteen(23, embers=False), overlay), "block", "houtskoolsteen_kaasknabbelerts.png")
    portal = tex.portal_frames()
    save(portal, "block", "barbecuether_portaal.png")
    with open(os.path.join(h.TEX, "block", "barbecuether_portaal.png.mcmeta"), "w") as f:
        json.dump({"animation": {"frametime": 2, "interpolate": True}}, f)
    for part in ("still", "flow"):
        save(tex.kaasfrituursaus_frames(h.vanilla(f"block/lava_{part}")), "block", f"kaasfrituursaus_{part}.png")
        with zipfile.ZipFile(os.path.join("build", "moddev", "artifacts", "neoforge-21.1.251-client-extra-aka-minecraft-resources.jar")) as jar:
            meta = json.loads(jar.read(f"assets/minecraft/textures/block/lava_{part}.png.mcmeta"))
        with open(os.path.join(h.TEX, "block", f"kaasfrituursaus_{part}.png.mcmeta"), "w") as f:
            json.dump(meta, f)

    # --- models and blockstates ---
    for b in SIMPLE:
        h.simple_block(b)
    for b in PILLARS:
        h.w(f"{A}/models/block/{b}.json", {"parent": "minecraft:block/cube_column", "textures": {
            "end": f"guhs:block/{b}_top", "side": f"guhs:block/{b}"}})
        h.w(f"{A}/models/block/{b}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal", "textures": {
            "end": f"guhs:block/{b}_top", "side": f"guhs:block/{b}"}})
        h.w(f"{A}/blockstates/{b}.json", {"variants": {
            "axis=y": {"model": f"guhs:block/{b}"},
            "axis=z": {"model": f"guhs:block/{b}_horizontal", "x": 90},
            "axis=x": {"model": f"guhs:block/{b}_horizontal", "x": 90, "y": 90}}})
        h.w(f"{A}/models/item/{b}.json", {"parent": f"guhs:block/{b}"})
    h.simple_block("as_blok")
    for b in ("pindasaus_nylium", "mosterd_nylium"):
        h.w(f"{A}/models/block/{b}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": f"guhs:block/{b}", "bottom": "guhs:block/houtskoolsteen", "side": f"guhs:block/{b}_side"}})
        h.w(f"{A}/blockstates/{b}.json", {"variants": {"": {"model": f"guhs:block/{b}"}}})
        h.w(f"{A}/models/item/{b}.json", {"parent": f"guhs:block/{b}"})
    for p in PLANTS:
        h.w(f"{A}/models/block/{p}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                          "textures": {"cross": f"guhs:block/{p}"}})
        h.w(f"{A}/blockstates/{p}.json", {"variants": {"": {"model": f"guhs:block/{p}"}}})
        h.item_model(p, f"guhs:block/{p}")
    # stairs, slab, wall, fence of the bricks
    t = "guhs:block/houtskoolsteen_stenen"
    n = "houtskoolsteen_stenen"
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        h.w(f"{A}/models/block/{n}_trap{suffix}.json", {"parent": f"minecraft:block/{parent}",
                                                          "textures": {"bottom": t, "top": t, "side": t}})
    h.w(f"{A}/blockstates/{n}_trap.json", {"variants": stair_states(f"guhs:block/{n}_trap")})
    h.w(f"{A}/models/item/{n}_trap.json", {"parent": f"guhs:block/{n}_trap"})
    h.w(f"{A}/models/block/{n}_plaat.json", {"parent": "minecraft:block/slab", "textures": {"bottom": t, "top": t, "side": t}})
    h.w(f"{A}/models/block/{n}_plaat_top.json", {"parent": "minecraft:block/slab_top", "textures": {"bottom": t, "top": t, "side": t}})
    h.w(f"{A}/blockstates/{n}_plaat.json", {"variants": {"type=bottom": {"model": f"guhs:block/{n}_plaat"},
                                                         "type=top": {"model": f"guhs:block/{n}_plaat_top"},
                                                         "type=double": {"model": f"guhs:block/{n}"}}})
    h.w(f"{A}/models/item/{n}_plaat.json", {"parent": f"guhs:block/{n}_plaat"})
    for part, parent in (("post", "template_wall_post"), ("side", "template_wall_side"), ("side_tall", "template_wall_side_tall")):
        h.w(f"{A}/models/block/{n}_muur_{part}.json", {"parent": f"minecraft:block/{parent}", "textures": {"wall": t}})
    h.w(f"{A}/models/block/{n}_muur_inventory.json", {"parent": "minecraft:block/wall_inventory", "textures": {"wall": t}})
    h.w(f"{A}/blockstates/{n}_muur.json", {"multipart": wall_multipart(f"guhs:block/{n}_muur")})
    h.w(f"{A}/models/item/{n}_muur.json", {"parent": f"guhs:block/{n}_muur_inventory"})
    for part, parent in (("post", "fence_post"), ("side", "fence_side")):
        h.w(f"{A}/models/block/{n}_hek_{part}.json", {"parent": f"minecraft:block/{parent}", "textures": {"texture": t}})
    h.w(f"{A}/models/block/{n}_hek_inventory.json", {"parent": "minecraft:block/fence_inventory", "textures": {"texture": t}})
    h.w(f"{A}/blockstates/{n}_hek.json", {"multipart": [{"apply": {"model": f"guhs:block/{n}_hek_post"}}] + [
        {"when": {d: "true"}, "apply": {"model": f"guhs:block/{n}_hek_side", "y": r, "uvlock": True}}
        for d, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))]})
    h.w(f"{A}/models/item/{n}_hek.json", {"parent": f"guhs:block/{n}_hek_inventory"})
    # the grill bars (iron-bars models)
    bt = "guhs:block/roosterijzer_tralies"
    for part in ("post_ends", "post", "cap", "cap_alt", "side", "side_alt"):
        h.w(f"{A}/models/block/roosterijzer_tralies_{part}.json", {"parent": f"minecraft:block/iron_bars_{part}",
                                                                   "render_type": "minecraft:cutout_mipped",
                                                                   "textures": {"particle": bt, "bars": bt, "edge": bt}})
    tb = "guhs:block/roosterijzer_tralies_"
    h.w(f"{A}/blockstates/roosterijzer_tralies.json", {"multipart": [
        {"apply": {"model": tb + "post_ends"}},
        {"when": {"north": "false", "east": "false", "south": "false", "west": "false"}, "apply": {"model": tb + "post"}},
        {"when": {"north": "true", "east": "false", "south": "false", "west": "false"}, "apply": {"model": tb + "cap"}},
        {"when": {"north": "false", "east": "true", "south": "false", "west": "false"}, "apply": {"model": tb + "cap", "y": 90}},
        {"when": {"north": "false", "east": "false", "south": "true", "west": "false"}, "apply": {"model": tb + "cap_alt"}},
        {"when": {"north": "false", "east": "false", "south": "false", "west": "true"}, "apply": {"model": tb + "cap_alt", "y": 90}},
        {"when": {"north": "true"}, "apply": {"model": tb + "side"}},
        {"when": {"east": "true"}, "apply": {"model": tb + "side", "y": 90}},
        {"when": {"south": "true"}, "apply": {"model": tb + "side_alt"}},
        {"when": {"west": "true"}, "apply": {"model": tb + "side_alt", "y": 90}}]})
    h.item_model("roosterijzer_tralies", bt)
    # the sauce puddle (a 1-pixel layer) and the smoke vent (a low grate)
    h.w(f"{A}/models/block/pindasausplasje.json", {"parent": "minecraft:block/carpet", "textures": {"wool": "guhs:block/pindasausplasje"}})
    h.w(f"{A}/blockstates/pindasausplasje.json", {"variants": {"": [{"model": "guhs:block/pindasausplasje", "y": r} for r in (0, 90, 180, 270)]}})
    h.w(f"{A}/models/item/pindasausplasje.json", {"parent": "guhs:block/pindasausplasje"})
    h.w(f"{A}/models/block/rookgat.json", {"parent": "minecraft:block/block", "textures": {
        "particle": "guhs:block/rookgat_side", "top": "guhs:block/rookgat_top", "side": "guhs:block/rookgat_side"},
        "elements": [{"from": [0, 0, 0], "to": [16, 6, 16], "faces": {
            "down": {"texture": "#side", "cullface": "down"}, "up": {"texture": "#top"},
            **{d: {"uv": [0, 10, 16, 16], "texture": "#side", "cullface": d} for d in ("north", "south", "east", "west")}}}]})
    h.w(f"{A}/blockstates/rookgat.json", {"variants": {"": {"model": "guhs:block/rookgat"}}})
    h.w(f"{A}/models/item/rookgat.json", {"parent": "guhs:block/rookgat"})
    # the portal and the frying sauce
    h.w(f"{A}/models/block/barbecuether_portaal_ns.json", {"parent": "minecraft:block/nether_portal_ns", "render_type": "minecraft:translucent",
                                                            "textures": {"particle": "guhs:block/barbecuether_portaal", "portal": "guhs:block/barbecuether_portaal"}})
    h.w(f"{A}/models/block/barbecuether_portaal_ew.json", {"parent": "minecraft:block/nether_portal_ew", "render_type": "minecraft:translucent",
                                                            "textures": {"particle": "guhs:block/barbecuether_portaal", "portal": "guhs:block/barbecuether_portaal"}})
    h.w(f"{A}/blockstates/barbecuether_portaal.json", {"variants": {"axis=x": {"model": "guhs:block/barbecuether_portaal_ns"},
                                                                    "axis=z": {"model": "guhs:block/barbecuether_portaal_ew"}}})
    h.w(f"{A}/blockstates/kaasfrituursaus.json", {"variants": {"": {"model": "guhs:block/kaasfrituursaus"}}})
    h.w(f"{A}/models/block/kaasfrituursaus.json", {"textures": {"particle": "guhs:block/kaasfrituursaus_still"}})

    # --- loot ---
    silk = [{"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
        {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]
    for b in ALL_BLOCKS:
        if b in ("gloeikool", "houtskoolsteen_kaasknabbelerts", "pindasaus_nylium", "mosterd_nylium", "houtskoolsteen_stenen_plaat"):
            continue
        h.self_drop(b)

    def alt(block, other, fns=None):
        entry = {"type": "minecraft:item", "name": other}
        if fns:
            entry["functions"] = fns
        h.w(f"{D}/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": f"guhs:{block}", "conditions": silk}, entry]}]}]})
    alt("gloeikool", "guhs:gloeikoolgruis", [
        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 4}},
        {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:uniform_bonus_count", "parameters": {"bonusMultiplier": 1}},
        {"function": "minecraft:limit_count", "limit": {"min": 1, "max": 4}}, {"function": "minecraft:explosion_decay"}])
    alt("houtskoolsteen_kaasknabbelerts", "guhs:kaas_knabbels", [
        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 6}},
        {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
        {"function": "minecraft:explosion_decay"}])
    alt("pindasaus_nylium", "guhs:houtskoolsteen")
    alt("mosterd_nylium", "guhs:houtskoolsteen")
    h.w(f"{D}/loot_table/blocks/houtskoolsteen_stenen_plaat.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "guhs:houtskoolsteen_stenen_plaat", "functions": [
            {"function": "minecraft:set_count", "count": 2, "conditions": [{"condition": "minecraft:block_state_property",
                                                                             "block": "guhs:houtskoolsteen_stenen_plaat",
                                                                             "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}]})

    # --- tags ---
    pick = ["houtskoolsteen", "houtskoolsteen_stenen", "houtskoolsteen_stenen_trap", "houtskoolsteen_stenen_plaat",
            "houtskoolsteen_stenen_muur", "houtskoolsteen_stenen_hek", "gebarsten_houtskoolsteen_stenen", "gebeitelde_houtskoolsteen_stenen",
            "roosterijzer", "roosterijzer_pilaar", "gepolijst_roosterijzer", "roosterijzer_tralies", "pindasaus_nylium", "mosterd_nylium",
            "rookgat", "verkoold_guhbot", "houtskoolsteen_kaasknabbelerts", "grillkool"]
    h.add_tag("minecraft/tags/block/mineable/pickaxe", [f"guhs:{b}" for b in pick])
    h.add_tag("minecraft/tags/block/mineable/shovel", ["guhs:as_blok", "guhs:as_aarde"])
    h.add_tag("minecraft/tags/block/mineable/axe", ["guhs:sate_stam", "guhs:worst_stam"])
    h.add_tag("minecraft/tags/block/mineable/hoe", ["guhs:sate_vlees", "guhs:mosterd_blok", "guhs:uienlicht"])
    h.add_tag("minecraft/tags/block/needs_diamond_tool", ["guhs:grillkool"])
    h.add_tag("minecraft/tags/block/nylium", ["guhs:pindasaus_nylium", "guhs:mosterd_nylium"])
    h.add_tag("minecraft/tags/block/stairs", ["guhs:houtskoolsteen_stenen_trap"])
    h.add_tag("minecraft/tags/block/slabs", ["guhs:houtskoolsteen_stenen_plaat"])
    h.add_tag("minecraft/tags/block/walls", ["guhs:houtskoolsteen_stenen_muur"])
    h.add_tag("minecraft/tags/block/fences", ["guhs:houtskoolsteen_stenen_hek"])
    h.add_tag("minecraft/tags/item/stairs", ["guhs:houtskoolsteen_stenen_trap"])
    h.add_tag("minecraft/tags/item/slabs", ["guhs:houtskoolsteen_stenen_plaat"])
    h.add_tag("minecraft/tags/item/walls", ["guhs:houtskoolsteen_stenen_muur"])
    h.add_tag("minecraft/tags/item/fences", ["guhs:houtskoolsteen_stenen_hek"])
    h.add_tag("minecraft/tags/block/soul_fire_base_blocks", ["guhs:as_blok", "guhs:as_aarde"])
    h.add_tag("minecraft/tags/block/soul_speed_blocks", ["guhs:as_blok", "guhs:as_aarde"])
    h.add_tag("minecraft/tags/block/infiniburn_overworld", ["guhs:houtskoolsteen"])
    h.add_tag("minecraft/tags/block/dragon_immune", ["guhs:grillkool", "guhs:barbecuether_portaal"])
    h.add_tag("minecraft/tags/block/wither_immune", ["guhs:barbecuether_portaal"])
    h.add_tag("minecraft/tags/block/portals", ["guhs:barbecuether_portaal"])
    h.add_tag("minecraft/tags/block/replaceable_by_trees", ["guhs:pindascheutjes", "guhs:mosterdscheutjes", "guhs:smeulkooltjes"])
    h.add_tag("minecraft/tags/block/enderman_holdable", ["guhs:pindasaus_nylium", "guhs:mosterd_nylium", "guhs:sate_zwammetje", "guhs:worst_zwammetje"])
    h.add_tag("guhs/tags/block/barbecuegrot_vervangbaar", ["guhs:houtskoolsteen", "guhs:as_blok", "guhs:as_aarde", "guhs:roosterijzer",
                                                           "guhs:roosterijzer_pilaar", "guhs:pindasaus_nylium", "guhs:mosterd_nylium",
                                                           "guhs:sate_vlees", "guhs:mosterd_blok", "guhs:houtskoolsteen_kaasknabbelerts",
                                                           "guhs:gloeikool", "minecraft:gravel"])

    # --- recipes ---
    h.shaped("houtskoolsteen_stenen", ["HH", "HH"], {"H": "guhs:houtskoolsteen"}, "guhs:houtskoolsteen_stenen", 4)
    h.shaped("houtskoolsteen_stenen_trap", ["S  ", "SS ", "SSS"], {"S": "guhs:houtskoolsteen_stenen"}, "guhs:houtskoolsteen_stenen_trap", 4)
    h.shaped("houtskoolsteen_stenen_plaat", ["SSS"], {"S": "guhs:houtskoolsteen_stenen"}, "guhs:houtskoolsteen_stenen_plaat", 6)
    h.shaped("houtskoolsteen_stenen_muur", ["SSS", "SSS"], {"S": "guhs:houtskoolsteen_stenen"}, "guhs:houtskoolsteen_stenen_muur", 6)
    h.shaped("houtskoolsteen_stenen_hek", ["SHS", "SHS"], {"S": "guhs:houtskoolsteen_stenen", "H": "guhs:houtskoolsteen"},
             "guhs:houtskoolsteen_stenen_hek", 6)
    h.shaped("gebeitelde_houtskoolsteen_stenen", ["P", "P"], {"P": "guhs:houtskoolsteen_stenen_plaat"}, "guhs:gebeitelde_houtskoolsteen_stenen")
    h.shaped("gepolijst_roosterijzer", ["RR", "RR"], {"R": "guhs:roosterijzer"}, "guhs:gepolijst_roosterijzer", 4)
    h.shaped("roosterijzer_pilaar", ["P", "P"], {"P": "guhs:gepolijst_roosterijzer"}, "guhs:roosterijzer_pilaar", 2)
    h.shaped("roosterijzer_tralies", ["RRR", "RRR"], {"R": "guhs:gepolijst_roosterijzer"}, "guhs:roosterijzer_tralies", 16)
    h.shaped("gloeikool", ["GG", "GG"], {"G": "guhs:gloeikoolgruis"}, "guhs:gloeikool")
    h.shaped("rookgat", ["T", "R"], {"T": "guhs:roosterijzer_tralies", "R": "guhs:gloeikool"}, "guhs:rookgat")
    h.shapeless("aanmaakblokje", ["guhs:grillguh_recept", "minecraft:charcoal", "minecraft:flint", "guhs:kaas_knabbels"], "guhs:aanmaakblokje")
    h.shapeless("kaasknabbelsate", ["minecraft:stick", "guhs:kaas_knabbels", "guhs:kaas_knabbels", "guhs:kaas_knabbels"], "guhs:kaasknabbelsate")
    for kind, typ, time in (("smelting", "minecraft:smelting", 200), ("smoking", "minecraft:smoking", 100),
                            ("campfire_cooking", "minecraft:campfire_cooking", 600)):
        h.w(f"{D}/recipe/gegrilde_kaasknabbelsate_{kind}.json", {"type": typ, "category": "food", "ingredient": {"item": "guhs:kaasknabbelsate"},
                                                                "result": {"id": "guhs:gegrilde_kaasknabbelsate"}, "experience": 0.35, "cookingtime": time})
    h.w(f"{D}/recipe/gebarsten_houtskoolsteen_stenen.json", {"type": "minecraft:smelting", "category": "blocks",
                                                             "ingredient": {"item": "guhs:houtskoolsteen_stenen"},
                                                             "result": {"id": "guhs:gebarsten_houtskoolsteen_stenen"}, "experience": 0.1, "cookingtime": 200})
    h.w(f"{D}/recipe/charcoal_van_houtskoolsteen.json", {"type": "minecraft:smelting", "category": "misc",
                                                         "ingredient": {"item": "guhs:houtskoolsteen"},
                                                         "result": {"id": "minecraft:charcoal"}, "experience": 0.1, "cookingtime": 200})
    for result, count, src in (("houtskoolsteen_stenen", 1, "houtskoolsteen"), ("houtskoolsteen_stenen_trap", 1, "houtskoolsteen_stenen"),
                               ("houtskoolsteen_stenen_plaat", 2, "houtskoolsteen_stenen"), ("houtskoolsteen_stenen_muur", 1, "houtskoolsteen_stenen"),
                               ("gebeitelde_houtskoolsteen_stenen", 1, "houtskoolsteen_stenen"), ("gepolijst_roosterijzer", 1, "roosterijzer"),
                               ("roosterijzer_pilaar", 1, "roosterijzer")):
        h.w(f"{D}/recipe/{result}_steenzagen_{src}.json", {"type": "minecraft:stonecutting", "ingredient": {"item": f"guhs:{src}"},
                                                          "result": {"id": f"guhs:{result}", "count": count}})


def stair_states(m):
    out = {}
    for facing, rot in (("east", 0), ("south", 90), ("west", 180), ("north", 270)):
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                model = m + ("" if shape == "straight" else "_inner" if shape.startswith("inner") else "_outer")
                y = rot
                if shape in ("inner_left", "outer_left"):
                    y = (rot + 270) % 360
                if half == "top":
                    if shape in ("inner_right", "outer_right"):
                        y = (rot + 90) % 360
                    elif shape in ("inner_left", "outer_left"):
                        y = rot
                v = {"model": model}
                if half == "top":
                    v["x"] = 180
                if y:
                    v["y"] = y
                if half == "top" or y:
                    v["uvlock"] = True
                out[f"facing={facing},half={half},shape={shape}"] = v
    return out


def wall_multipart(m):
    parts = [{"when": {"up": "true"}, "apply": {"model": m + "_post"}}]
    for d, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        parts.append({"when": {d: "low"}, "apply": {"model": m + "_side", "y": r, "uvlock": True}})
        parts.append({"when": {d: "tall"}, "apply": {"model": m + "_side_tall", "y": r, "uvlock": True}})
    for p in parts[1:]:
        if p["apply"]["y"] == 0:
            del p["apply"]["y"]
    return parts


# =====================================================================================================================
# items
# =====================================================================================================================
def items(h):
    for name, rows in tex.ICONS.items():
        h.save(h.grid(rows, tex.ITEM_PAL), "item", f"{name}.png")
        h.item_model(name, parent="minecraft:item/handheld" if "sate" in name else "minecraft:item/generated")
    bucket = Image.open(os.path.join(h.TEX, "item", "kaas_saus_bucket.png")).convert("RGBA")
    b2 = h.recolour(bucket, hue=0.05, sat=1.3, val=0.95, only=lambda hh, s, v: (hh > 0.05) & (hh < 0.2) & (s > 0.3))
    h.save(b2, "item", "kaasfrituursaus_bucket.png")
    h.item_model("kaasfrituursaus_bucket")


# =====================================================================================================================
# the dimension
# =====================================================================================================================
def y_above(anchor, add=False):
    return {"type": "minecraft:y_above", "anchor": anchor, "surface_depth_multiplier": 0, "add_stone_depth": add}


def noise_cond(n, lo):
    return {"type": "minecraft:noise_threshold", "noise": f"minecraft:{n}", "min_threshold": lo, "max_threshold": 1.7976931348623157e308}


def depth(add, surface):
    return {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": add, "secondary_depth_range": 0, "surface_type": surface}


def not_(c):
    return {"type": "minecraft:not", "invert": c}


def if_(c, r):
    return {"type": "minecraft:condition", "if_true": c, "then_run": r}


def seq(*r):
    return {"type": "minecraft:sequence", "sequence": list(r)}


def blk(name, props=None):
    state = {"Name": name}
    if props:
        state["Properties"] = props
    return {"type": "minecraft:block", "result_state": state}


def biome_is(b):
    return {"type": "minecraft:biome", "biome_is": [f"guhs:{b}"]}


def surface_rule():
    """The Nether's surface rule (SurfaceRuleData.nether), with the Barbecuether's blocks and biomes."""
    HK = blk("guhs:houtskoolsteen")
    PIL = blk("guhs:roosterijzer_pilaar", {"axis": "y"})
    RI = blk("guhs:roosterijzer")
    AS = blk("guhs:as_blok")
    AA = blk("guhs:as_aarde")
    GRAVEL = blk("guhs:as_aarde")
    SAUS = blk("guhs:kaasfrituursaus", {"level": "0"})
    BEDROCK = blk("minecraft:bedrock")
    c = y_above({"absolute": 31})
    c1 = y_above({"absolute": 32})
    c2 = y_above({"absolute": 30}, True)
    c3 = not_(y_above({"absolute": 35}, True))
    c4 = y_above({"below_top": 5})
    hole = {"type": "minecraft:hole"}
    c6, c7, c8 = noise_cond("soul_sand_layer", -0.012), noise_cond("gravel_layer", -0.012), noise_cond("patch", -0.012)
    c9, c10, c11 = noise_cond("netherrack", 0.54), noise_cond("nether_wart", 1.17), noise_cond("nether_state_selector", 0.0)
    gravel_patch = if_(c8, if_(c2, if_(c3, GRAVEL)))
    UNDER_CEILING, UNDER_FLOOR, ON_FLOOR = depth(True, "ceiling"), depth(True, "floor"), depth(False, "floor")
    return seq(
        if_({"type": "minecraft:vertical_gradient", "random_name": "minecraft:bedrock_floor", "true_at_and_below": {"above_bottom": 0},
             "false_at_and_above": {"above_bottom": 5}}, BEDROCK),
        if_(not_({"type": "minecraft:vertical_gradient", "random_name": "minecraft:bedrock_roof", "true_at_and_below": {"below_top": 5},
                  "false_at_and_above": {"below_top": 0}}), BEDROCK),
        if_(c4, HK),
        if_(biome_is("rookdelta"), seq(if_(UNDER_CEILING, PIL), if_(UNDER_FLOOR, seq(gravel_patch, if_(c11, PIL), RI)))),
        if_(biome_is("asdal"), seq(if_(UNDER_CEILING, seq(if_(c11, AS), AA)), if_(UNDER_FLOOR, seq(gravel_patch, if_(c11, AS), AA)))),
        if_(ON_FLOOR, seq(
            if_(not_(c1), if_(hole, SAUS)),
            if_(biome_is("worstenwoud"), if_(not_(c9), if_(c, seq(if_(c10, blk("guhs:mosterd_blok")), blk("guhs:mosterd_nylium"))))),
            if_(biome_is("satebos"), if_(not_(c9), if_(c, seq(if_(c10, blk("guhs:sate_vlees")), blk("guhs:pindasaus_nylium"))))))),
        if_(biome_is("houtskoolvlakte"), seq(
            if_(UNDER_FLOOR, if_(c6, seq(if_(not_(hole), if_(c2, if_(c3, AS))), HK))),
            if_(ON_FLOOR, if_(c, if_(c3, if_(c7, seq(if_(c1, GRAVEL), if_(not_(hole), GRAVEL)))))))),
        HK)


def base_noise():
    """The Nether's final density (slide over the 3D noise between floor and ceiling), with its own 3D noise."""
    noise3d = {"type": "minecraft:old_blended_noise", "xz_scale": 0.25, "y_scale": 0.375, "xz_factor": 80.0, "y_factor": 60.0,
               "smear_scale_multiplier": 8.0}
    inner = {"type": "minecraft:add", "argument1": 0.9375, "argument2": {"type": "minecraft:mul",
             "argument1": {"type": "minecraft:y_clamped_gradient", "from_y": 104, "to_y": 128, "from_value": 1.0, "to_value": 0.0},
             "argument2": {"type": "minecraft:add", "argument1": -0.9375, "argument2": noise3d}}}
    outer = {"type": "minecraft:add", "argument1": 2.5, "argument2": {"type": "minecraft:mul",
             "argument1": {"type": "minecraft:y_clamped_gradient", "from_y": -8, "to_y": 24, "from_value": 0.0, "to_value": 1.0},
             "argument2": {"type": "minecraft:add", "argument1": -2.5, "argument2": inner}}}
    return {"type": "minecraft:squeeze", "argument": {"type": "minecraft:mul", "argument1": 0.64,
                                                      "argument2": {"type": "minecraft:interpolated", "argument": {
                                                          "type": "minecraft:blend_density", "argument": outer}}}}


def shifted(noise):
    return {"type": "minecraft:shifted_noise", "noise": noise, "xz_scale": 0.5, "y_scale": 0.0,
            "shift_x": "minecraft:shift_x", "shift_y": 0.0, "shift_z": "minecraft:shift_z"}


def dimension(h):
    D = h.D
    h.w(f"{D}/dimension_type/{NAME}.json", {
        "ultrawarm": True, "natural": False, "piglin_safe": False, "respawn_anchor_works": True, "bed_works": False,
        "has_raids": False, "has_skylight": False, "has_ceiling": True, "coordinate_scale": 8.0, "ambient_light": 0.1,
        "logical_height": 128, "min_y": 0, "height": 128, "infiniburn": "#minecraft:infiniburn_nether",
        "effects": "guhs:barbecuether", "fixed_time": 18000, "monster_spawn_light_level": 7, "monster_spawn_block_light_limit": 15})
    h.w(f"{D}/dimension/{NAME}.json", {"type": f"guhs:{NAME}", "generator": {
        "type": "minecraft:noise", "settings": f"guhs:{NAME}", "biome_source": {"type": "minecraft:multi_noise", "biomes": [
            {"biome": f"guhs:{b}", "parameters": {"temperature": t, "humidity": hu, "continentalness": 0.0, "erosion": 0.0,
                                                    "depth": 0.0, "weirdness": 0.0, "offset": off}}
            for b, (t, hu, off) in BIOME_POINTS.items()]}}})
    # own biome noises (finer than the overworld's, so all five biomes show up within a few hundred blocks)
    h.w(f"{D}/worldgen/noise/barbecue_temperature.json", {"firstOctave": -7, "amplitudes": [1.5, 0.0, 1.0, 0.0]})
    h.w(f"{D}/worldgen/noise/barbecue_vegetation.json", {"firstOctave": -7, "amplitudes": [1.0, 1.0, 0.0, 0.0]})
    zero = 0.0
    h.w(f"{D}/worldgen/noise_settings/{NAME}.json", {
        "sea_level": 32, "disable_mob_generation": False, "aquifers_enabled": False, "ore_veins_enabled": False,
        "legacy_random_source": False,
        "default_block": {"Name": "guhs:houtskoolsteen"},
        "default_fluid": {"Name": "guhs:kaasfrituursaus", "Properties": {"level": "0"}},
        "noise": {"min_y": 0, "height": 128, "size_horizontal": 1, "size_vertical": 2},
        "noise_router": {
            "barrier": zero, "fluid_level_floodedness": zero, "fluid_level_spread": zero, "lava": zero,
            "temperature": shifted("guhs:barbecue_temperature"), "vegetation": shifted("guhs:barbecue_vegetation"),
            "continents": zero, "erosion": zero, "depth": zero, "ridges": zero,
            "initial_density_without_jaggedness": zero, "final_density": base_noise(),
            "vein_toggle": zero, "vein_ridged": zero, "vein_gap": zero},
        "spawn_target": [],
        "surface_rule": surface_rule()})
    # caves: the Nether's cave carver, in our rock, without lava pools
    h.w(f"{D}/worldgen/configured_carver/barbecuegrot.json", {"type": "minecraft:nether_cave", "config": {
        "probability": 0.2, "y": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 0}, "max_inclusive": {"below_top": 1}},
        "yScale": 0.5, "lava_level": {"absolute": -10}, "replaceable": "#guhs:barbecuegrot_vervangbaar",
        "horizontal_radius_multiplier": 1.0, "vertical_radius_multiplier": 1.0, "floor_level": -0.7}})
    features(h)
    biomes(h)
    h.add_tag("guhs/tags/worldgen/biome/is_barbecuether", [f"guhs:{b}" for b in BIOMES])


def cf(h, name, typ, config):
    h.w(f"{h.D}/worldgen/configured_feature/{name}.json", {"type": typ, "config": config})


def pf(h, name, feature, placement):
    h.w(f"{h.D}/worldgen/placed_feature/{name}.json", {"feature": feature if ":" in feature else f"guhs:{feature}", "placement": placement})


def count(n):
    return {"type": "minecraft:count", "count": n}


def uniform(lo, hi):
    return {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}


def height(lo, hi):
    return {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}


SQUARE, BIOME = {"type": "minecraft:in_square"}, {"type": "minecraft:biome"}


def layers(n):
    return {"type": "minecraft:count_on_every_layer", "count": n}


def patch(block, props=None, tries=32, xz=6, y=2, on=None):
    """A random_patch of one block, only where it can stay (and optionally only on these blocks)."""
    state = {"Name": block}
    if props:
        state["Properties"] = props
    preds = [{"type": "minecraft:matching_blocks", "blocks": "minecraft:air"}]
    if on:
        preds.append({"type": "minecraft:matching_blocks", "offset": [0, -1, 0], "blocks": on})
    else:
        preds.append({"type": "minecraft:would_survive", "state": state})
    return {"tries": tries, "xz_spread": xz, "y_spread": y, "feature": {
        "feature": {"type": "minecraft:simple_block", "config": {"to_place": {"type": "minecraft:simple_state_provider", "state": state}}},
        "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": preds}}]}}


def spring(rock, hole):
    return {"state": {"Name": "guhs:kaasfrituursaus", "Properties": {"falling": "true"}}, "requires_block_below": False,
            "rock_count": rock, "hole_count": hole, "valid_blocks": ["guhs:houtskoolsteen", "guhs:as_aarde", "guhs:roosterijzer"]}


# the placed features of each generation step, in one global order (every biome uses them in this order)
STEP_ORDER = {
    4: ["bbq_delta", "bbq_roosterpilaren", "bbq_guhfossiel"],
    7: ["bbq_bron_open", "bbq_vuurtjes", "bbq_asvuurtjes", "bbq_gloeikool_extra", "bbq_gloeikool", "bbq_roosterijzer_blobs",
        "bbq_kaasknabbelerts", "bbq_bron_dicht"],
    9: ["bbq_satespiezen", "bbq_braadworsten", "bbq_satebos_planten", "bbq_worstenwoud_planten", "bbq_smeulkooltjes",
        "bbq_pindasausplasjes", "bbq_rookgaten"],
}
BIOME_FEATURES = {
    "houtskoolvlakte": {"bbq_bron_open", "bbq_vuurtjes", "bbq_gloeikool_extra", "bbq_gloeikool", "bbq_kaasknabbelerts",
                        "bbq_bron_dicht", "bbq_smeulkooltjes"},
    "satebos": {"bbq_bron_open", "bbq_vuurtjes", "bbq_gloeikool_extra", "bbq_gloeikool", "bbq_kaasknabbelerts", "bbq_bron_dicht",
                "bbq_satespiezen", "bbq_satebos_planten", "bbq_pindasausplasjes"},
    "worstenwoud": {"bbq_bron_open", "bbq_gloeikool_extra", "bbq_gloeikool", "bbq_kaasknabbelerts", "bbq_bron_dicht",
                    "bbq_braadworsten", "bbq_worstenwoud_planten"},
    "asdal": {"bbq_guhfossiel", "bbq_asvuurtjes", "bbq_gloeikool_extra", "bbq_gloeikool", "bbq_kaasknabbelerts", "bbq_bron_dicht"},
    "rookdelta": {"bbq_delta", "bbq_roosterpilaren", "bbq_bron_open", "bbq_vuurtjes", "bbq_gloeikool_extra", "bbq_gloeikool",
                  "bbq_roosterijzer_blobs", "bbq_kaasknabbelerts", "bbq_bron_dicht", "bbq_rookgaten"},
}


def features(h):
    none = {}
    cf(h, "bbq_delta", "minecraft:delta_feature", {
        "contents": {"Name": "guhs:kaasfrituursaus", "Properties": {"level": "0"}},
        "rim": {"Name": "guhs:roosterijzer_pilaar", "Properties": {"axis": "y"}}, "size": uniform(3, 7), "rim_size": uniform(0, 2)})
    pf(h, "bbq_delta", "bbq_delta", [layers(40), BIOME])
    cf(h, "bbq_roosterpilaren", "guhs:roosterpilaren", none)
    pf(h, "bbq_roosterpilaren", "bbq_roosterpilaren", [layers(3), BIOME])
    cf(h, "bbq_guhfossiel", "guhs:verkoold_guhfossiel", none)
    pf(h, "bbq_guhfossiel", "bbq_guhfossiel", [{"type": "minecraft:rarity_filter", "chance": 3}, layers(1), BIOME])
    cf(h, "bbq_bron_open", "minecraft:spring_feature", spring(4, 1))
    pf(h, "bbq_bron_open", "bbq_bron_open", [count(8), SQUARE, height({"above_bottom": 4}, {"below_top": 8}), BIOME])
    cf(h, "bbq_bron_dicht", "minecraft:spring_feature", spring(5, 0))
    pf(h, "bbq_bron_dicht", "bbq_bron_dicht", [count(16), SQUARE, height({"above_bottom": 10}, {"below_top": 10}), BIOME])
    fire = {"age": "0", "east": "false", "north": "false", "south": "false", "up": "false", "west": "false"}
    cf(h, "bbq_vuurtjes", "minecraft:random_patch", patch("minecraft:fire", fire, 64, 7, 3, on="guhs:houtskoolsteen"))
    pf(h, "bbq_vuurtjes", "bbq_vuurtjes", [count(uniform(0, 5)), SQUARE, height({"above_bottom": 4}, {"below_top": 4}), BIOME])
    cf(h, "bbq_asvuurtjes", "minecraft:random_patch", patch("minecraft:soul_fire", None, 64, 7, 3, on=["guhs:as_blok", "guhs:as_aarde"]))
    pf(h, "bbq_asvuurtjes", "bbq_asvuurtjes", [count(uniform(0, 5)), SQUARE, height({"above_bottom": 4}, {"below_top": 4}), BIOME])
    cf(h, "bbq_gloeikool", "guhs:gloeikool_klomp", none)
    pf(h, "bbq_gloeikool_extra", "bbq_gloeikool", [count({"type": "minecraft:biased_to_bottom", "min_inclusive": 0, "max_inclusive": 9}),
                                                   SQUARE, height({"above_bottom": 4}, {"below_top": 4}), BIOME])
    pf(h, "bbq_gloeikool", "bbq_gloeikool", [count(10), SQUARE, height({"above_bottom": 0}, {"below_top": 0}), BIOME])
    cf(h, "bbq_roosterijzer_blobs", "minecraft:netherrack_replace_blobs", {
        "target": {"Name": "guhs:houtskoolsteen"}, "state": {"Name": "guhs:roosterijzer"}, "radius": uniform(3, 7)})
    pf(h, "bbq_roosterijzer_blobs", "bbq_roosterijzer_blobs", [count(25), SQUARE, height({"above_bottom": 0}, {"below_top": 0}), BIOME])
    cf(h, "bbq_kaasknabbelerts", "minecraft:ore", {"size": 10, "discard_chance_on_air_exposure": 0.0, "targets": [
        {"target": {"predicate_type": "minecraft:block_match", "block": "guhs:houtskoolsteen"},
         "state": {"Name": "guhs:houtskoolsteen_kaasknabbelerts"}}]})
    pf(h, "bbq_kaasknabbelerts", "bbq_kaasknabbelerts", [count(10), SQUARE, height({"above_bottom": 10}, {"below_top": 10}), BIOME])
    cf(h, "sate_spies_gekweekt", "guhs:sate_spies", none)          # (bone meal on a saté sprout; see BarbecuetherFeature)
    cf(h, "braadworst_gekweekt", "guhs:braadworst", none)
    pf(h, "bbq_satespiezen", "sate_spies_gekweekt", [layers(8), BIOME])
    pf(h, "bbq_braadworsten", "braadworst_gekweekt", [layers(8), BIOME])
    cf(h, "bbq_satebos_planten", "minecraft:nether_forest_vegetation", {"state_provider": {
        "type": "minecraft:weighted_state_provider", "entries": [
            {"weight": 87, "data": {"Name": "guhs:pindascheutjes"}}, {"weight": 11, "data": {"Name": "guhs:sate_zwammetje"}},
            {"weight": 6, "data": {"Name": "guhs:smeulkooltjes"}}]}, "spread_width": 8, "spread_height": 4})
    pf(h, "bbq_satebos_planten", "bbq_satebos_planten", [layers(6), BIOME])
    cf(h, "bbq_worstenwoud_planten", "minecraft:nether_forest_vegetation", {"state_provider": {
        "type": "minecraft:weighted_state_provider", "entries": [
            {"weight": 85, "data": {"Name": "guhs:mosterdscheutjes"}}, {"weight": 13, "data": {"Name": "guhs:worst_zwammetje"}}]},
        "spread_width": 8, "spread_height": 4})
    pf(h, "bbq_worstenwoud_planten", "bbq_worstenwoud_planten", [layers(5), BIOME])
    cf(h, "bbq_smeulkooltjes", "minecraft:random_patch", patch("guhs:smeulkooltjes", None, 24, 6, 2))
    pf(h, "bbq_smeulkooltjes", "bbq_smeulkooltjes", [layers(1), BIOME])
    cf(h, "bbq_pindasausplasjes", "minecraft:random_patch", patch("guhs:pindasausplasje", None, 16, 4, 1))
    pf(h, "bbq_pindasausplasjes", "bbq_pindasausplasjes", [layers(2), BIOME])
    cf(h, "bbq_rookgaten", "minecraft:random_patch", patch("guhs:rookgat", None, 8, 5, 2, on=["guhs:roosterijzer", "guhs:roosterijzer_pilaar"]))
    pf(h, "bbq_rookgaten", "bbq_rookgaten", [{"type": "minecraft:rarity_filter", "chance": 2}, layers(1), BIOME])


def biomes(h):
    for b, (nl, fog, sky, particle, sounds, mika) in BIOMES.items():
        steps = [[] for _ in range(11)]
        for step, order in STEP_ORDER.items():
            steps[step] = [f"guhs:{f}" for f in order if f in BIOME_FEATURES[b]]
        h.w(f"{h.D}/worldgen/biome/{b}.json", {
            "has_precipitation": False, "temperature": 2.0, "downfall": 0.0,
            "effects": {
                "fog_color": fog, "sky_color": sky, "water_color": 0x3F76E4, "water_fog_color": 0x050533,
                "particle": particle,
                "ambient_sound": f"minecraft:ambient.{sounds}.loop",
                "additions_sound": {"sound": f"minecraft:ambient.{sounds}.additions", "tick_chance": 0.0111},
                "mood_sound": {"sound": f"minecraft:ambient.{sounds}.mood", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
                "music": {"sound": f"minecraft:music.nether.{sounds}", "min_delay": 12000, "max_delay": 24000, "replace_current_music": False}},
            "spawners": {"monster": [{"type": "guhs:nether_mika", "weight": mika, "minCount": 1, "maxCount": 2}],
                         "creature": [], "ambient": [], "axolotls": [], "underground_water_creature": [], "water_creature": [],
                         "water_ambient": [], "misc": []},
            "spawn_costs": {},
            "carvers": {"air": ["guhs:barbecuegrot"]},
            "features": steps})
        h.lang(f"biome.guhs.{b}", nl, nl)


# =====================================================================================================================
# the barbecue pits
# =====================================================================================================================
def structures(h):
    D = h.D
    problems, pits = put.build_all(h)
    h.Structure((9, 20, 9)).save("bbq_testkamer")          # (an empty room for the GameTests)
    if problems:
        print("barbecueput geometry check found problems:\n  " + "\n  ".join(problems[:40]))
        raise SystemExit("barbecuether: fix the barbecueput templates (see the geometry check above)")
    h.w(f"{D}/worldgen/structure/barbecueput.json", {
        "type": "guhs:barbecueput", "biomes": "#guhs:has_structure/barbecueput", "step": "surface_structures",
        "spawn_overrides": {}, "terrain_adaptation": "beard_thin", "start_pool": "guhs:barbecueput/start",
        "start_jigsaw_name": put.MIDDEN, "check_radius": 20, "max_height_difference": 12, "headroom": 10,
        # bbq2 (ring-h1): in the open (the Guhmensie) only the small pits from now on: a new big pit there is always the
        # middle of a Knabbelgouw (guhs:knabbelgouw, the second structure of this set; features/ring_h1.py)
        "surface_pool": "guhs:barbecueput/klein"})
    def pool(*templates):
        return {"fallback": "minecraft:empty", "elements": [
            {"weight": wgt, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:{name}",
                                        "projection": "rigid", "processors": "minecraft:empty"}} for name, wgt in templates]}
    h.w(f"{D}/worldgen/template_pool/barbecueput/start.json", pool(("barbecueput_groot", 2), ("barbecueput_klein_a", 1), ("barbecueput_klein_b", 1)))
    h.w(f"{D}/worldgen/template_pool/barbecueput/klein.json", pool(("barbecueput_klein_a", 1), ("barbecueput_klein_b", 1)))
    # rare, but findable: about as rare as the guh kermis (many spots are too steep, so the grid is a bit tighter).
    # bbq2 (ring-h1): the set holds two structures. In the Guhmensie the game picks one of them per spot (3 : 2 for the
    # Knabbelgouw, as the big pit had 2 of 4 and the Knabbelgouw needs more flat room; where it does not fit, a small pit
    # comes); in the Guhbarbecuether the Knabbelgouw's biomes never match, so there it is always guhs:barbecueput.
    h.w(f"{D}/worldgen/structure_set/barbecueput.json", {"structures": [{"structure": "guhs:barbecueput", "weight": 2},
                                                                        {"structure": "guhs:knabbelgouw", "weight": 3}],
                                                         "placement": {"type": "minecraft:random_spread", "spacing": 36, "separation": 14,
                                                                       "salt": 27012027}})
    h.w(f"{D}/tags/worldgen/biome/has_structure/barbecueput.json", {"values": [f"guhs:{b}" for b in h.GUHMENSION_LAND + ["mikas_biome"]]
                                                                    + [f"guhs:{b}" for b in BIOMES]})
    loot = {
        "barbecueput_groot": [("guhs:grillkool", 4, 1, 3), ("minecraft:coal_block", 3, 1, 2), ("guhs:kaas_saus_bucket", 1, 1, 1),
                              ("guhs:gloeikoolgruis", 4, 2, 6), ("guhs:gegrilde_kaasknabbelsate", 4, 1, 3), ("guhs:guhbraadworst", 3, 1, 3),
                              ("guhs:kaas_knabbels", 5, 4, 12), ("minecraft:charcoal", 3, 2, 6), ("minecraft:gold_nugget", 3, 3, 9)],
        "barbecueput_klein": [("guhs:grillkool", 4, 1, 2), ("minecraft:coal_block", 2, 1, 1), ("guhs:aanmaakblokje", 2, 1, 1),
                              ("guhs:gloeikoolgruis", 3, 1, 4), ("guhs:kaasknabbelsate", 4, 1, 3), ("guhs:kaas_knabbels", 5, 3, 8),
                              ("minecraft:charcoal", 4, 1, 4), ("minecraft:gold_nugget", 3, 2, 6), ("minecraft:flint", 2, 1, 3)],
    }
    for table, entries in loot.items():
        h.w(f"{D}/loot_table/chests/{table}.json", {"type": "minecraft:chest", "pools": [
            {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
                {"type": "minecraft:item", "name": item, "weight": wgt, "functions": h.count_fn(lo, hi)}
                for item, wgt, lo, hi in entries]}]})
    return pits


# =====================================================================================================================
# lang, advancements
# =====================================================================================================================
LANG = {
    "block.guhs.houtskoolsteen": "Houtskoolsteen",
    "block.guhs.houtskoolsteen_stenen": "Houtskoolsteen stenen",
    "block.guhs.houtskoolsteen_stenen_trap": "Houtskoolsteen stenen trap",
    "block.guhs.houtskoolsteen_stenen_plaat": "Houtskoolsteen stenen plaat",
    "block.guhs.houtskoolsteen_stenen_muur": "Houtskoolsteen stenen muur",
    "block.guhs.houtskoolsteen_stenen_hek": "Houtskoolsteen stenen hek",
    "block.guhs.gebarsten_houtskoolsteen_stenen": "Gebarsten houtskoolsteen stenen",
    "block.guhs.gebeitelde_houtskoolsteen_stenen": "Gebeitelde guh-houtskoolsteen stenen",
    "block.guhs.roosterijzer": "Roosterijzer",
    "block.guhs.roosterijzer_pilaar": "Roosterijzer pilaar",
    "block.guhs.gepolijst_roosterijzer": "Gepolijst roosterijzer",
    "block.guhs.roosterijzer_tralies": "Roosterijzer tralies",
    "block.guhs.gloeikool": "Gloeikool",
    "block.guhs.as_blok": "Asblok",
    "block.guhs.as_aarde": "Asaarde",
    "block.guhs.sate_stam": "Satéstam",
    "block.guhs.pindasaus_nylium": "Pindasaus-nylium",
    "block.guhs.worst_stam": "Worststam",
    "block.guhs.mosterd_nylium": "Mosterd-nylium",
    "block.guhs.sate_vlees": "Satévlees",
    "block.guhs.mosterd_blok": "Mosterdblok",
    "block.guhs.uienlicht": "Uienlicht",
    "block.guhs.pindascheutjes": "Pindascheutjes",
    "block.guhs.mosterdscheutjes": "Mosterdscheutjes",
    "block.guhs.sate_zwammetje": "Satézwammetje",
    "block.guhs.worst_zwammetje": "Worstzwammetje",
    "block.guhs.smeulkooltjes": "Smeulkooltjes",
    "block.guhs.pindasausplasje": "Pindasausplasje",
    "block.guhs.rookgat": "Rookgat",
    "block.guhs.verkoold_guhbot": "Verkoold guhbot",
    "block.guhs.houtskoolsteen_kaasknabbelerts": "Houtskoolsteen-kaasknabbelerts",
    "block.guhs.grillkool": "Grillkool",
    "block.guhs.barbecuether_portaal": "Barbecuetherportaal",
    "block.guhs.kaasfrituursaus": "Kaasfrituursaus",
    "fluid_type.guhs.kaasfrituursaus": "Kaasfrituursaus",
    "item.guhs.kaasfrituursaus_bucket": "Emmer kaasfrituursaus",
    "item.guhs.aanmaakblokje": "Aanmaakblokje",
    "item.guhs.aanmaakblokje.lore": "Klik in een grillkoolframe (Guhmensie of Barbecuether): de barbecue gaat aan! Werkt ook als aansteker.",
    "item.guhs.gloeikoolgruis": "Gloeikoolgruis",
    "item.guhs.grillguh_recept": "Grillguhs geheime recept",
    "item.guhs.grillguh_recept.lore": "Met houtskool, vuursteen en een kaasknabbel maak je er Aanmaakblokjes mee. Het recept blijft in het rooster liggen.",
    "item.guhs.kaasknabbelsate": "Kaasknabbelsaté",
    "item.guhs.gegrilde_kaasknabbelsate": "Gegrilde kaasknabbelsaté",
    "item.guhs.guhbraadworst": "Guhbraadworst",
    "item.guhs.grill_koksmuts": "Grillguh-koksmuts",
    "item.guhs.grill_schort": "Barbecueschort",
    "item.guhs.grill_halsdoek": "Koksdoekje",
    "entity.guhs.guh_npc.grillguh": "Grillguh",
    "structure.guhs.barbecueput": "Barbecueput",
    "structure.guhs.barbecueput.tooltip": "Kapotte barbecues met een grillkoolframe. In de grote woont de Grillguh (ook in de Barbecuether zelf)",
    "gui.guhs.superkompas.barbecue": "Barbecue",
    "gui.guhs.superkompas.barbecue.tooltip": "Alles om te barbecueën, van de Guhmensie tot diep in de Barbecuether",
    "gui.guhs.guhdex.rarity.grillguh": "Zeldzaamheid: Zeldzaam (grote barbecueput)",
    "gui.guhs.guhdex.info.grillguh": "Een guh-kok met een torenhoge koksmuts en een schort. De Mika's hebben zijn Aanmaakblokjes gejat! Help hem en hij leert je zijn geheime grillrecept.",
    "dimension.guhs.barbecuether": "De Guhbarbecuether",
    "quest.guhs.barbecuether.wrong_dimension": "Njeg... dit barbecueportaal wil alleen branden in de Guhmensie of de Barbecuether!",
    "quest.guhs.barbecuether.te_warm": "Te warm om te slapen, njeg!",
    # the Grillguh
    "quest.guhs.grillguh.hallo1": "NJEG, mijn barbecue! De Mika's hebben mijn Aanmaakblokjes gejat! En mijn mooie grillkoolframe is ook nog kapot, vads...",
    "quest.guhs.grillguh.hallo2": "Wil jij me helpen? Eerst het frame: daar moet grillkool in. Grillkool maak je zo: laat kaassaus over een blok steenkool stromen. Sssss... grillkool! Zet de ontbrekende stukken terug (breek de gebarsten stenen eruit), dan praten we verder.",
    "quest.guhs.grillguh.frame_hint": "Het grillkoolframe is nog niet heel, njeg. Tip: kaassaus over een blok steenkool = grillkool. De gebarsten stenen in het frame moeten er ook uit!",
    "quest.guhs.grillguh.frame_ok": "VAHOEG, het frame is weer heel! Nu nog mijn Aanmaakblokjes... Die vadsige Mika's zitten in een Mika-kamp. Versla er een paar: zolang je mij helpt, laat elke Mika daar een Aanmaakblokje vallen.",
    "quest.guhs.grillguh.blokjes_hint": "Mijn Aanmaakblokjes liggen bij de Mika's in een Mika-kamp (het superkompas weet er een te vinden). Versla een Mika daar en neem er een mee terug!",
    "quest.guhs.grillguh.blokjes_terug": "Mijn Aanmaakblokjes! Njeg njeg, wat ruiken ze naar Mika. Klik er nu mee in het grillkoolframe: steek mijn put aan!",
    "quest.guhs.grillguh.aansteken": "Klik met een Aanmaakblokje in het grillkoolframe, dan brandt mijn barbecue weer!",
    "quest.guhs.grillguh.klaar1": "VAHOEG, mijn barbecue brandt weer! Ruik je dat? Kaasfrituursaus, helemaal uit de Guhbarbecuether!",
    "quest.guhs.grillguh.klaar2": "Hier: mijn geheime grillrecept (daarmee maak je zelf Aanmaakblokjes) en een koksmuts voor je guh. En kom maar eens langs in mijn winkeltje!",
    "quest.guhs.grillguh.recept_weer": "Njeg, ben je mijn recept kwijt? Hier, ik had nog een kopietje. Niet aan de Mika's laten zien!",
    "quest.guhs.grillguh.mika_drop": "De Mika laat een gestolen Aanmaakblokje vallen!",
    "quest.guhs.grillguh.tip0": "Door het portaal is het heet, heel heet. Kaasfrituursaus brandt, dus niet in springen, vads!",
    "quest.guhs.grillguh.tip1": "In het Satébos groeien reuzensatéspiezen, in het Worstenwoud staan braadworsten zo groot als bomen. VAHOEG!",
    "quest.guhs.grillguh.tip2": "Een bed in de Barbecuether? Njeg! Veel te warm om te slapen.",
    "quest.guhs.grillguh.tip3": "Eén stap daar is acht stappen hier. Handig om snel te reizen, als je tegen de hitte kunt!",
    "quest.guhs.grillguh.tip4": "Kaassaus op kaasfrituursaus wordt grillkool. Zo maak je in de Barbecuether altijd een nieuw frame.",
    # advancements
    "advancements.guhs.barbecuether.root.title": "De Guhbarbecuether",
    "advancements.guhs.barbecuether.root.description": "Grillkool, Aanmaakblokjes en een heel hete dimensie",
    "advancements.guhs.barbecuether.barbecueput.title": "Wie heeft hier gebarbecued?",
    "advancements.guhs.barbecuether.barbecueput.description": "Vind een kapotte barbecueput",
    "advancements.guhs.barbecuether.grillkool.title": "Zwart als houtskool",
    "advancements.guhs.barbecuether.grillkool.description": "Maak grillkool: kaassaus over een blok steenkool",
    "advancements.guhs.barbecuether.grillguh.title": "De Grillguh helpt",
    "advancements.guhs.barbecuether.grillguh.description": "Steek de barbecue van de Grillguh weer aan en krijg zijn geheime recept",
    "advancements.guhs.barbecuether.binnen.title": "Het is hier heet, njeg!",
    "advancements.guhs.barbecuether.binnen.description": "Stap door een barbecueportaal de Guhbarbecuether in",
    "advancements.guhs.barbecuether.alle_biomen.title": "Rondje barbecue",
    "advancements.guhs.barbecuether.alle_biomen.description": "Bezoek alle vijf de biomen van de Barbecuether",
    "advancements.guhs.barbecuether.emmer.title": "Frituurvads in een emmer",
    "advancements.guhs.barbecuether.emmer.description": "Schep een emmer kaasfrituursaus",
    "advancements.guhs.barbecuether.gloeikool.title": "Lekker warm licht",
    "advancements.guhs.barbecuether.gloeikool.description": "Pak een blok gloeikool van het plafond van de Barbecuether",
    "advancements.guhs.barbecuether.te_warm.title": "Te warm om te slapen",
    "advancements.guhs.barbecuether.te_warm.description": "Probeer in de Barbecuether in bed te gaan. Njeg!",
    "advancements.guhs.barbecuether.pakje.title": "Chef Guh",
    "advancements.guhs.barbecuether.pakje.description": "Koop het hele kokspakje bij de Grillguh",
    "advancements.guhs.barbecuether.terug.title": "Even afkoelen",
    "advancements.guhs.barbecuether.terug.description": "Ga door een barbecueportaal terug naar de Guhmensie",
}

QUEST_ADVANCEMENTS = ["grill_gevonden", "grill_frame", "grill_blokjes", "grill_aangestoken", "seen_grillguh", "te_warm"]


def advancements(h):
    D = h.D
    in_dim = {"trigger": "minecraft:changed_dimension", "conditions": {"to": f"guhs:{NAME}"}}

    def adv(name, parent, icon, frame, criteria, requirements=None, hidden=False):
        data = {"display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.barbecuether.{name}.title"},
                            "description": {"translate": f"advancements.guhs.barbecuether.{name}.description"},
                            "frame": frame, "show_toast": True, "announce_to_chat": name != "root", "hidden": hidden},
                "criteria": criteria}
        if parent:
            data["parent"] = f"guhs:barbecuether/{parent}"
        else:
            data["display"]["background"] = "guhs:textures/block/houtskoolsteen_stenen.png"
            data["display"]["show_toast"] = False
        if requirements:
            data["requirements"] = requirements
        h.w(f"{D}/advancement/barbecuether/{name}.json", data)

    def has(item):
        return {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}}

    adv("root", None, "guhs:grillkool", "task", {"grillkool": has("guhs:grillkool"), "blokje": has("guhs:aanmaakblokje"), "binnen": in_dim},
        [["grillkool", "blokje", "binnen"]])
    adv("barbecueput", "root", "guhs:gebeitelde_houtskoolsteen_stenen", "task",
        {"done": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": "guhs:barbecueput"}}}}})
    adv("grillkool", "barbecueput", "guhs:grillkool", "task", {"done": has("guhs:grillkool")})
    adv("grillguh", "grillkool", "guhs:grillguh_recept", "goal", {"done": has("guhs:grillguh_recept")})
    adv("pakje", "grillguh", "guhs:grill_koksmuts", "challenge", {c: has(f"guhs:{c}") for c in CLOTHES})
    adv("binnen", "root", "guhs:aanmaakblokje", "goal", {"done": in_dim})
    adv("alle_biomen", "binnen", "guhs:sate_stam", "challenge", {
        b: {"trigger": "minecraft:location", "conditions": {"player": {"location": {"biomes": f"guhs:{b}"}}}} for b in BIOMES})
    adv("emmer", "binnen", "guhs:kaasfrituursaus_bucket", "task", {"done": has("guhs:kaasfrituursaus_bucket")})
    adv("gloeikool", "binnen", "guhs:gloeikool", "task", {"done": has("guhs:gloeikool")})
    adv("te_warm", "binnen", "minecraft:red_bed", "task",
        {"done": {"trigger": "minecraft:impossible"}}, hidden=True)
    adv("terug", "binnen", "guhs:houtskoolsteen", "task",
        {"done": {"trigger": "minecraft:changed_dimension", "conditions": {"from": f"guhs:{NAME}", "to": "guhs:guhmension"}}})
    # the "too warm" one is granted together with its hidden quest advancement (see BarbecuetherEvents.tooHot)
    for name in QUEST_ADVANCEMENTS:
        h.w(f"{D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


# =====================================================================================================================
# build
# =====================================================================================================================
def build(h):
    blocks(h)
    items(h)
    dimension(h)
    structures(h)
    advancements(h)
    # the Grillguh: a warm, toasty guh with a white chef's hat and apron painted on (the removable ones are guh clothes)
    src = Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    npc = h.recolour(src, hue=0.04, sat=1.1, val=1.02, only=h.pinkish).convert("RGBA")
    paint_chef(npc)
    h.save(npc, "entity", "npc_grillguh.png")
    for key, text in LANG.items():
        h.lang(key, text, text)
    selfcheck_assets(h)


def paint_chef(im):
    """The sitting guh's texture (UV 128, 4 px per unit): white on top of the head (the toque), a charcoal apron with
    an orange trim on the front of the body."""
    px = im.load()
    S = im.width // 128

    def rect(u, v, w, hgt, colour, rows=None):
        for y in range(int(v * S), int((v + hgt) * S)):
            if rows is not None and not rows(y - int(v * S), int(hgt * S)):
                continue
            for x in range(int(u * S), int((u + w) * S)):
                if px[x, y][3]:
                    px[x, y] = colour
    white = (250, 250, 248, 255)
    # head tops ("up" faces of the head cubes)
    for (u, v, w, hgt) in ((16, 32, 16, 8), (88, 32, 11, 9), (40, 46, 14, 13), (0, 59, 13, 11)):
        rect(u, v, w, hgt, white)
    # the top rows of the head's side faces (the band of the toque), not the face itself
    for (u, v, w, hgt) in ((106, 23, 16, 9), (0, 32, 8, 9), (8, 32, 8, 9), (59, 32, 11, 14), (70, 32, 9, 14), (79, 32, 9, 14),
                           (0, 46, 14, 11), (14, 46, 13, 11), (27, 46, 13, 11), (81, 46, 13, 12), (94, 46, 11, 12), (105, 46, 11, 12)):
        rect(u, v, w, hgt, (200, 40, 30, 255), rows=lambda r, total: r < max(2, total // 6))
    # the apron on the front of the body cubes (north faces)
    for (u, v, w, hgt) in ((0, 0, 10, 9), (50, 0, 7, 12), (90, 0, 8, 10), (16, 12, 9, 11)):
        rect(u, v, w, hgt, (58, 52, 56, 255))
        rect(u, v, w, hgt, (220, 90, 30, 255), rows=lambda r, total: r < 2 or r >= total - 2)


def selfcheck_assets(h):
    """check_assets.py only knows the registry classes: this checks our own blocks and items."""
    A = h.A
    missing = []
    for b in ALL_BLOCKS + ["barbecuether_portaal", "kaasfrituursaus"]:
        if not os.path.exists(f"{A}/blockstates/{b}.json"):
            missing.append(f"blockstate {b}")
        if b not in ("barbecuether_portaal", "kaasfrituursaus") and not os.path.exists(f"{A}/models/item/{b}.json"):
            missing.append(f"item model {b}")
        if f"block.guhs.{b}" not in h.NL:
            missing.append(f"lang block.guhs.{b}")
    for i in ITEMS + CLOTHES:
        if f"item.guhs.{i}" not in h.NL:
            missing.append(f"lang item.guhs.{i}")
    for i in ITEMS:
        if not os.path.exists(f"{A}/models/item/{i}.json") or not os.path.exists(f"{A}/textures/item/{i}.png"):
            missing.append(f"item {i}")
    for path, _dirs, files in os.walk(f"{A}/models/block"):
        for f in files:
            if f.startswith(tuple(ALL_BLOCKS)) or f.startswith("barbecuether_portaal"):
                model = json.load(open(os.path.join(path, f), encoding="utf-8"))
                for t in model.get("textures", {}).values():
                    if t.startswith("guhs:") and not os.path.exists(f"{A}/textures/{t[5:]}.png"):
                        missing.append(f"texture {t} ({f})")
    if missing:
        raise SystemExit(f"barbecuether assets missing: {missing}")


# =====================================================================================================================
# FTB quests (rows y = 60 and 61.5)
# =====================================================================================================================
def ftb(fq):
    q, item, adv, structure, biome, dim = fq.q, fq.item, fq.adv, fq.structure, fq.biome, fq.dim
    y = 60
    q("bbq_put", "Wie heeft hier gebarbecued?", "Ergens in de Guhmensie liggen kapotte barbecues met een half grillkoolframe: de &6barbecueputten&r. Het superkompas (Barbecue > Barbecueput) wijst de weg.",
      # (bbq2: the task is the visible advancement, not the structure guhs:barbecueput: a new big pit in the Guhmensie is the
      # middle of a Knabbelgouw, a structure of its own, and features/ring_h1.py lets that advancement count there too)
      "guhs:gebeitelde_houtskoolsteen_stenen", [adv("guhs:barbecuether/barbecueput")], rewards=(("guhs:kaas_knabbels", 12),), x=-8, y=y, shape="hexagon", xp=100)
    q("bbq_grillguh", "NJEG, mijn barbecue!", "In de grote barbecueput zit de &dGrillguh&r. De Mika's hebben zijn Aanmaakblokjes gejat! Praat met hem.",
      "guhs:grill_koksmuts", [adv("grill_gevonden")], x=-6, y=y)
    q("bbq_grillkool", "Zwart als houtskool", "Laat &6kaassaus&r over een &8blok steenkool&r stromen: sssss... &8grillkool&r! Maak er een paar.",
      "guhs:grillkool", [item("guhs:grillkool", 4)], rewards=(("minecraft:coal_block", 2),), x=-4, y=y)
    q("bbq_frame", "Het frame weer heel", "Zet grillkool in de gaten van het frame van de Grillguh (de gebarsten stenen moeten eruit) en vertel het hem.",
      "guhs:grillkool", [adv("grill_frame")], x=-2, y=y)
    q("bbq_blokjes", "Gejatte Aanmaakblokjes", "Ga naar een &cMika-kamp&r. Zolang je de Grillguh helpt, laat elke Mika die je daar verslaat een Aanmaakblokje vallen. Breng er een terug!",
      "guhs:aanmaakblokje", [adv("grill_blokjes")], x=0, y=y)
    q("bbq_aan", "VAHOEG, hij brandt weer!", "Klik met een Aanmaakblokje in het grillkoolframe van de Grillguh. Zijn barbecue brandt weer, en je krijgt zijn geheime grillrecept!",
      "guhs:grillguh_recept", [adv("grill_aangestoken")], rewards=(("guhs:gegrilde_kaasknabbelsate", 4),), x=2, y=y, shape="gear", xp=300)
    q("bbq_pakje", "Chef Guh", "Koop de Grillguh-koksmuts, het barbecueschort en het koksdoekje bij de Grillguh en trek ze je guh aan.",
      "guhs:grill_schort", [item("guhs:grill_koksmuts"), item("guhs:grill_schort"), item("guhs:grill_halsdoek")],
      rewards=(("guhs:guhbraadworst", 6),), x=4, y=y, shape="rsquare", xp=150)
    q("bbq_dex", "Grillguh in de Guhdex", "Ga vlak naast de Grillguh staan: dan komt hij in je Guhdex.", "guhs:guhdex",
      [adv("seen_grillguh")], rewards=(("guhs:kaas_knabbels", 16),), x=6, y=y)
    y2 = 61.5
    q("bbq_dimensie", "Het is hier heet, njeg!", "Steek een grillkoolframe (4x5 tot 23x23) in de Guhmensie aan met een Aanmaakblokje en stap de &6Guhbarbecuether&r in. Eén stap daar is acht stappen in de Guhmensie!",
      "guhs:aanmaakblokje", [dim("guhs:barbecuether")], rewards=(("guhs:gegrilde_kaasknabbelsate", 4),), x=-8, y=y2, shape="octagon", xp=200)
    for i, (b, (nl, *_rest)) in enumerate(BIOMES.items()):
        q(f"bbq_biome_{b}", nl, f"Bezoek het bioom &6{nl}&r in de Guhbarbecuether.", "minecraft:filled_map", [biome(b)],
          rewards=(("guhs:kaas_knabbels", 6),), x=-6 + i * 1.5, y=y2, shape="circle")
    q("bbq_emmer", "Frituurvads in een emmer", "Schep een emmer &6kaasfrituursaus&r uit een saus-zee. Heet!", "guhs:kaasfrituursaus_bucket",
      [item("guhs:kaasfrituursaus_bucket")], x=1.5, y=y2)
    q("bbq_gloeikool", "Lekker warm licht", "Hak &6gloeikool&r van het plafond van de Barbecuether (met zijdezacht, of verzamel 4 gloeikoolgruis).",
      "guhs:gloeikool", [item("guhs:gloeikool")], x=3, y=y2)
    q("bbq_warm", "Te warm om te slapen", "Probeer in de Barbecuether in bed te gaan. Wat zegt je guh?", "minecraft:red_bed", [adv("te_warm")],
      x=4.5, y=y2, shape="diamond")
    q("bbq_bouwen", "Barbecue-bouwer", "Maak houtskoolsteen stenen, roosterijzer tralies en een rookgat: bouw je eigen barbecue!",
      "guhs:roosterijzer_tralies", [item("guhs:houtskoolsteen_stenen", 16), item("guhs:roosterijzer_tralies", 4), item("guhs:rookgat")],
      x=6, y=y2, shape="diamond")
