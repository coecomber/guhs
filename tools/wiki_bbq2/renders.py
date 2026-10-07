"""
The wiki pictures of bbq2 (Guh-technologie, the Guhbarbecuether buildings, In de ban van de Knabbelring, Super Guhrio).

renders(r) is called by tools/wiki_renders.py main_bbq2 (r is that module, r.OUT the folder docs/wiki/img). Everything is
drawn from the mod's own files, like every other wiki picture:
  npc_<kind>.png            the 27 new characters, each from its own model (guh_npc_<kind>.geo.json)
  <entity>.png              the new creatures (GeckoLib models, and the box models of Super Guhrio)
  guh_variant_<id>.png      Sam-guh and Guhshi (the guh model with their own bones)
  block_<id>.png, icon_<id>.png   every new block with a 3D model (the look of its item: a machine that is awake)
  structure_<id>.png        the new buildings, from their .nbt (the tiled ones put back together)

SPOILERS: the pictures of the story's later places and of the two big surprises are made too (structure pages and creature
pages need them), but the site only shows them behind a spoiler toggle and never in a list: see SPOILER_* in
tools/wiki_bbq2/__init__.py. Guhdalf is drawn grey and Araguh without his crown (NPC_HIDE of tools/make_ftbquests_art.py).
"""
import json
import os

import numpy as np

# kind -> the bones that are hidden (the same table as tools/make_ftbquests_art.py NPC_HIDE: a character's later look is a spoiler)
NPC_HIDE = {"guhdalf": ("wit_hoed", "wit_mantel", "wit_knop"), "araguh": ("araguh_kroon",)}

NPCS = ("uitvinderguh", "wachterguh", "knuffelmakerguh", "mika_oma", "stalknechtguh", "tolwachter_mika", "archeoloogguh", "mijnwerkerguh",
        "verzorgerguh", "kampbaasguh", "houthakkerguh", "marktmeester_mika", "torenwachterguh", "pepertelerguh", "guhdalf", "smikagol",
        "araguh", "leguhlas", "gimguh", "boromika", "merrie", "pippguh", "guhrond", "guhladriel", "sausuman", "padguh", "perzikguh")

# picture name -> (geo model, texture, glow mask or None, yaw, pitch, size)
GEO = {
    "sausloper": ("sausloper", "sausloper", None, 32, -14, 480),
    "sausblubje": ("sausblubje", "sausblubje", "sausblubje_glowmask", 32, -18, 400),
    "worstzwijntje": ("worstzwijntje", "worstzwijntje", None, 35, -16, 400),
    "bezorgguhtje": ("bezorgguhtje", "bezorgguhtje", None, 35, -14, 400),
    "smikagol": ("smikagol", "smikagol", None, 30, -12, 400),
    "knekel_ruiter": ("knekel_ruiter", "knekel_ruiter", None, 32, -14, 480),
    "paleizen_mopper_mika": ("paleizen_mopper_mika", "paleizen_mopper_mika", None, 30, -14, 400),
    "campingmarkt_kraam_mika": ("campingmarkt_kraam_mika", "campingmarkt_kraam_mika", None, 30, -14, 400),
    "ringh5_roosterwachter": ("ringh5_roosterwachter", "ringh5_roosterwachter", None, 30, -14, 400),
    "ringh4_elfenbootje": ("ringh4_elfenbootje", "ringh4_elfenbootje", None, 35, -22, 400),
    "oog_van_sausron": ("oog_van_sausron", "oog_van_sausron", "oog_van_sausron_glowmask", 20, -8, 480),
    "barbecuerog": ("barbecuerog", "barbecuerog", "barbecuerog_glowmask", 28, -8, 640),
}
# the box models of Super Guhrio (features/guhrio_modellen.quads): picture name -> (model, parts or None)
DOZEN = {"guhmba": ("guhmba", None), "schild_mika": ("schild_mika", None), "plof_mika": ("plof_mika", ["blok", "oren", "noppen"]),
         "hapbloem": ("hapbloem", None), "grote_nether_mika": ("grote_nether_mika", None)}

# structure id -> (template or tile folder, quarter turns, cut: None | height of the cut, name of the picture)
STRUCTUREN = {
    "oude_guhrad_centrale": ("oude_guhrad_centrale", 0, None), "fossiel_opgraving": ("fossiel_opgraving", 0, None),
    "zoutkristalmijn": ("zoutkristalmijn", 0, None), "sausloper_stal": ("sausloper_stal", 0, None),
    "grillcamping": ("grillcamping", 0, None), "mika_ruilmarkt": ("mika_ruilmarkt", 0, None),
    "rookguh_vuurtoren": ("rookguh_vuurtoren", 0, None), "pepertuin": ("pepertuin", 0, None),
    "mika_stal": ("mika_stal", 0, None), "mika_woonblokken": ("mika_woonblokken/", 0, None),
    "mika_brugpaleis": ("mika_brugpaleis/", 0, None), "knabbelgouw": ("knabbelgouw", 0, None),
    "ring_rustpunt": ("ring_rustpunt_afdakje", 0, None), "guhrio_kasteel": ("guhrio_kasteel/", 0, None),
    # the story's later places (the site hides them behind a spoiler toggle)
    "guhvendel": ("guhvendel", 0, None), "knabbelmoria": ("knabbelmoria", 0, "half"),
    "guhladriel_boomstad": ("guhladriel_boomstad/", 0, None), "zwarte_roosterpoort": ("zwarte_roosterpoort/", 0, "half"),
    "frituurberg": ("frituurberg/", 0, None), "sausuman_toren": ("sausuman_toren", 0, None),
}
# vanilla blocks in the new templates whose colour does not follow from their name
KLEUR_ALS = {"chain": "iron_block", "iron_chain": "iron_block", "lightning_rod": "copper_block", "campfire": "campfire_log_lit",
             "soul_campfire": "campfire_log", "lantern": "lantern", "soul_lantern": "soul_lantern", "hopper": "hopper_outside",
             "cauldron": "cauldron_side", "lava_cauldron": "cauldron_side", "anvil": "anvil", "bell": "gold_block", "ladder": "oak_planks",
             "lever": "cobblestone", "brewing_stand": "brewing_stand_base", "grindstone": "grindstone_side", "barrel": "barrel_side",
             "composter": "composter_side", "loom": "loom_side", "smoker": "smoker_side", "furnace": "furnace_side",
             "blast_furnace": "blast_furnace_side", "stonecutter": "stonecutter_side", "bookshelf": "bookshelf", "torch": "torch",
             "wall_torch": "torch", "soul_torch": "soul_torch", "soul_wall_torch": "soul_torch", "redstone_torch": "redstone_torch",
             "tripwire_hook": "oak_planks", "flower_pot": "flower_pot", "end_rod": "end_rod", "decorated_pot": "terracotta",
             "scaffolding": "scaffolding_top", "cake": "cake_top", "candle": "white_wool", "trapped_chest": "oak_planks",
             "jukebox": "jukebox_side", "note_block": "note_block", "dried_kelp_block": "dried_kelp_side", "bone_block": "bone_block_side",
             "basalt": "basalt_side", "polished_basalt": "polished_basalt_side", "quartz_block": "quartz_block_side",
             "quartz_pillar": "quartz_pillar", "smooth_quartz": "quartz_block_bottom", "hay_block": "hay_block_side",
             "magma_block": "magma", "snow": "snow", "vine": "vine", "kelp": "kelp", "sea_pickle": "sea_pickle", "moss_carpet": "moss_block",
             "pointed_dripstone": "dripstone_block", "big_dripleaf": "big_dripleaf_top", "small_dripleaf": "small_dripleaf_top",
             "sniffer_egg": "sniffer_egg_not_cracked_top", "smooth_stone_slab": "smooth_stone", "petrified_oak_slab": "oak_planks",
             "fire": None, "soul_fire": None, "light": None, "barrier": None, "structure_void": None, "jigsaw": None, "cave_air": None,
             "void_air": None, "redstone_wire": None, "tripwire": None, "string": None}


def _geo(r, name):
    return os.path.join(r.ASSETS, "geckolib", "models", "entity", name + ".geo.json")


def _save(r, im, name, crop=False):
    if crop:
        box = im.getbbox()
        if box:
            im = im.crop((max(0, box[0] - 8), max(0, box[1] - 8), min(im.width, box[2] + 8), min(im.height, box[3] + 8)))
    im.save(os.path.join(r.OUT, name + ".png"))
    print("rendered", name)


def _skin(r, tex, glow=None):
    """A texture (with its glow mask drawn over it, like the game does at night) as the array the renderer wants."""
    im = r.texture(f"guhs:entity/{tex}")
    if glow:
        im = r.Image.alpha_composite(im, r.texture(f"guhs:entity/{glow}"))
    return np.asarray(im).astype(np.float32)


# ---------------------------------------------------------------------------------------------------------------------
def npcs(r):
    for kind in NPCS:
        try:
            model = _geo(r, f"guh_npc_{kind}") if os.path.exists(_geo(r, f"guh_npc_{kind}")) else _geo(r, "guh_sitting")
            _save(r, r.render(r.geo_quads(model, f"guhs:entity/npc_{kind}", hide=NPC_HIDE.get(kind, ())), 28, -12, 360), f"npc_{kind}")
        except Exception as e:  # noqa: BLE001
            print("no render for npc", kind, e)


def creatures(r):
    for name, (model, tex, glow, yaw, pitch, size) in GEO.items():
        try:
            quads = r.geo_quads(_geo(r, model), _skin(r, tex, glow), show_only_variant_bones=("",))
            _save(r, r.render(quads, yaw, pitch, size), name)
        except Exception as e:  # noqa: BLE001
            print("no render for", name, e)
    try:
        from features import guhrio_modellen as gm
        for name, (model, delen) in DOZEN.items():
            try:
                _save(r, r.render(gm.quads(model, delen), 205, -12, 400), name)     # (these models look along +z: seen from the front-left)
            except Exception as e:  # noqa: BLE001
                print("no render for", name, e)
    except Exception as e:  # noqa: BLE001
        print("no Super Guhrio models:", e)
    # the two little Rookguhs of the stories look like the Rookguh
    for name in ("torenpeper_verdwaalde_rookguh", "ringh6_rookguh"):
        try:
            _save(r, r.render(r.rookguh_quads(), 30, -10, 360), name)
        except Exception as e:  # noqa: BLE001
            print("no render for", name, e)
    # Sam-guh and Guhshi: the guh model with their own bones and fur
    hide = ("saddle",) + tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
    for vid, bones in (("sam_guh", ("samguh",)), ("guhshi", ("guhshi",))):
        try:
            quads = r.geo_quads(_geo(r, "guh"), f"guhs:entity/guh_{vid}", hide=hide, show_only_variant_bones=bones)
            _save(r, r.render(quads, 35, -20, 512), f"guh_variant_{vid}")
            _save(r, r.render(quads, 150, -22, 400), f"guh_variant_{vid}_back")
        except Exception as e:  # noqa: BLE001
            print("no render for", vid, e)


# ---------------------------------------------------------------------------------------------------------------------
def nieuwe_blokken(r):
    """Every guhs block with a blockstate that has no picture yet and whose item shows a 3D model: (id, model to draw)."""
    out = []
    bs_dir = os.path.join(r.ASSETS, "blockstates")
    for f in sorted(os.listdir(bs_dir)):
        bid = f[:-5]
        if not _bbq2(bid):
            continue
        item = os.path.join(r.ASSETS, "models", "item", bid + ".json")
        ij = json.load(open(item, encoding="utf-8")) if os.path.exists(item) else {}
        parent = ij.get("parent", "")
        if parent.startswith("guhs:block/"):
            out.append((bid, parent))
        elif ij.get("elements"):
            out.append((bid, f"guhs:item/{bid}"))
    return out


PREFIXEN = ("paleizen_", "bestaand_", "fossielmijn_", "campingmarkt_", "torenpeper_", "ring_", "ringh3_", "ringh4_", "ringh5_", "ringh6_",
            "ringsausuman_", "guhrio_", "guhriow1_", "guhriow2_", "guhriow3_", "guhriobeloning_", "sausdieren_", "techquest_")
LOS = {"hapluikje", "knuffelgenerator", "disco_dynamo", "blubkacheltje", "gloeisterkern", "knabbelbatterij", "knabbelbuis",
       "knabbelbuis_richting", "knabbelbuis_filter", "opzuiger", "voorraadmeter", "snuffelsensor", "guhklok", "guhteller", "vadsmolen",
       "oogster", "knabbelaar", "neerzetter", "knutselmachine", "tekentafel", "plantagebak", "sauspomp", "sausslang", "sausvat",
       "brouwautomaat", "frituurautomaat", "grillkoolpers", "stepstation", "haltepaaltje", "grote_knabbelmachine", "knabbelmachine_beeldje",
       "elfentouw_haak", "oog_van_sausron_beeldje"}
# blocks that are bigger than one block: (yaw, pitch)
KIJK = {"knuffelgenerator": (30, -30), "disco_dynamo": (30, -32), "grote_knabbelmachine": (30, -16)}


def _bbq2(bid):
    return bid in LOS or bid.startswith(PREFIXEN)


def blocks(r, only=None):
    lijst = [x for x in nieuwe_blokken(r) if only is None or x[0] in only]
    deel = os.environ.get("WIKI_BBQ2_DEEL")          # "i/n": only every n-th block from the i-th on (to render in n processes at once)
    if deel:
        i, n = (int(v) for v in deel.split("/"))
        lijst = lijst[i::n]
    for bid, model in lijst:
        try:
            quads = r.model_quads(model)
            if not quads:
                continue
            yaw, pitch = KIJK.get(bid, (30, -24))        # (a machine's snoet is on its north side: this angle shows it, front-left)
            _save(r, r.render(quads, yaw, pitch, 320, margin=0.05), f"block_{bid}")
            _save(r, r.render(quads, yaw, -30, 256).resize((64, 64), r.Image.LANCZOS), f"icon_{bid}")
        except Exception as e:  # noqa: BLE001
            print("no render for block", bid, e)


# ---------------------------------------------------------------------------------------------------------------------
def _tiles(r, folder):
    """A building saved as 32 x 32 tiles (<folder>/stuk_i_j.nbt) put back together."""
    import make_structures as ms
    d = os.path.join("src", "main", "resources", "data", "guhs", "structure", folder)
    parts = {}
    for f in sorted(os.listdir(d)):
        if f.startswith("stuk_") and f.endswith(".nbt"):
            i, j = (int(v) for v in f[5:-4].split("_"))
            parts[(i, j)] = r.load_structure(f"{folder}/{f[:-4]}")
    if not parts:
        return None
    W = max(i * 32 + st.size[0] for (i, j), st in parts.items())
    D = max(j * 32 + st.size[2] for (i, j), st in parts.items())
    whole = ms.Structure((W, max(st.size[1] for st in parts.values()), D))
    for (i, j), st in parts.items():
        for (x, y, z), v in st.blocks.items():
            whole.blocks[(x + i * 32, y, z + j * 32)] = v
        whole.entities += [(x + i * 32, y, z + j * 32, nbt) for x, y, z, nbt in st.entities]
    return whole


def _vanilla(bid):
    """The texture a vanilla block's colour is read from when no texture has its name: (texture or None, known)."""
    b = bid[6:] if bid.startswith("waxed_") else bid
    if b.endswith(("_wall_banner", "_banner")):
        return b.replace("_wall_banner", "").replace("_banner", "") + "_wool"
    if b.endswith(("_wall_sign", "_hanging_sign", "_sign")):
        return b.replace("_wall_hanging_sign", "").replace("_wall_sign", "").replace("_hanging_sign", "").replace("_sign", "") + "_planks"
    if b.startswith("potted_"):
        return "flower_pot"
    if b.endswith("_candle_cake"):
        return "cake_top"
    if b.endswith("_cauldron"):
        return "cauldron_side"
    if b in ("carrots", "potatoes", "beetroots"):
        return b + "_stage3"
    if b.startswith("stripped_") and b.endswith("_wood"):
        return b[:-5] + "_log"
    for suffix in ("_stairs", "_slab", "_wall", "_grate", "_bulb"):
        if b.endswith(suffix):
            b = b[: -len(suffix)]
    b = {"quartz": "quartz_block_side", "smooth_quartz": "quartz_block_bottom", "brick": "bricks", "red_nether_brick": "red_nether_bricks",
         "nether_brick": "nether_bricks", "smooth_sandstone": "sandstone_top", "stone_brick": "stone_bricks", "mud_brick": "mud_bricks",
         "deepslate_brick": "deepslate_bricks", "deepslate_tile": "deepslate_tiles", "polished_blackstone_brick": "polished_blackstone_bricks",
         "copper": "copper_block"}.get(b, b)
    return b if b != bid else None


def _kleuren(r, st, onbekend):
    """A colour for every block of a template: our own blocks without a texture of their own name get the colour of their
    model's biggest face, vanilla blocks the texture KLEUR_ALS names; what is left is listed (and drawn grey)."""
    for name in sorted({v[0] for v in st.blocks.values()}):
        if name in r.SPECIAL_COLOURS or name in r.BLOCK_TEXTURES:
            continue
        ns, bid = name.split(":", 1)
        if ns == "minecraft" and bid in KLEUR_ALS:
            if KLEUR_ALS[bid] is None:
                r.SPECIAL_COLOURS[name] = None
            else:
                r.BLOCK_TEXTURES[name] = "block/" + KLEUR_ALS[bid]
            continue
        if r.block_colour(name) != (200, 0, 200):
            continue
        if ns == "minecraft" and _vanilla(bid):
            r.BLOCK_TEXTURES[name] = "block/" + _vanilla(bid)
            r.block_colour.cache_clear()
            if r.block_colour(name) != (200, 0, 200):
                continue
            del r.BLOCK_TEXTURES[name]
        if ns == "guhs":
            try:
                quads = r.model_quads(r.first_model(bid))
                r.BLOCK_TEXTURES[name] = max(quads, key=lambda q: np.linalg.norm(np.cross(q.u, q.v))).tex
                continue
            except Exception:  # noqa: BLE001
                pass
        onbekend.add(name)
        r.SPECIAL_COLOURS[name] = (150, 146, 150) if ns == "minecraft" else None
    r.block_colour.cache_clear()


def _sprites(r, st):
    """The sprites for the entities of a template: every NPC kind as its own picture (the entity id becomes npc_<kind>)."""
    hide = ("saddle",) + tuple(f"armor_{t}{p}" for t in ("iron", "diamond", "netherite") for p in ("", "_body"))
    spr = lambda quads, blocks, foot=0.85, pitch=-30: {"img": r.render(quads, 45, pitch, 256, margin=0.02), "blocks": blocks, "foot": foot}
    out = {}
    cache = _sprites.cache
    for i, (x, y, z, nbt) in enumerate(st.entities):
        eid = nbt.get("id", "").split(":")[-1]
        key = eid
        try:
            if eid == "guh_npc":
                kind = str(nbt.get("Kind", nbt.get("kind", ""))).lower()
                key = f"npc_{kind}"
                if key not in cache:
                    model = _geo(r, f"guh_npc_{kind}") if os.path.exists(_geo(r, f"guh_npc_{kind}")) else _geo(r, "guh_sitting")
                    cache[key] = spr(r.geo_quads(model, f"guhs:entity/npc_{kind}", hide=NPC_HIDE.get(kind, ())), 1.4, 0.95, -20)
                st.entities[i] = (x, y, z, dict(nbt, id="guhs:" + key))
            elif key not in cache:
                if eid == "guh":
                    cache[key] = spr(r.geo_quads(_geo(r, "guh"), "guhs:entity/guh", hide=hide), 1.0)
                elif eid in ("mika", "nether_mika"):
                    cache[key] = spr(r.geo_quads(_geo(r, "mika"), f"guhs:entity/{eid}"), 1.0)
                elif eid in GEO:
                    model, tex, glow = GEO[eid][:3]
                    cache[key] = spr(r.geo_quads(_geo(r, model), _skin(r, tex, glow), show_only_variant_bones=("",)), 1.4)
                else:
                    cache[key] = None
        except Exception as e:  # noqa: BLE001
            print("no sprite for", key, e)
            cache[key] = None
        if cache.get(key):
            out[key] = cache[key]
    return out


_sprites.cache = {}


def structures(r, only=None):
    import make_structures as ms
    r.SPECIAL_COLOURS.update({"guhs:kaasfrituursaus": (236, 120, 30), "guhs:borrelende_kaassaus": (226, 190, 60),
                              "guhs:barbecuether_portaal": (255, 110, 40), "minecraft:lava": (230, 100, 20),
                              "guhs:knabbelbuis": (196, 222, 232), "guhs:sausslang": (120, 96, 80),
                              "minecraft:birch_leaves": (128, 167, 85), "minecraft:dirt_path": (148, 121, 65)})
    # the Knabbelgouw stands in the Guhmensie, where the grass is pink (the biome's own grass colour)
    try:
        biome = json.load(open(os.path.join("src", "main", "resources", "data", "guhs", "worldgen", "biome", "guh_fields.json"), encoding="utf-8"))
        g = biome["effects"]["grass_color"]
        gras = ((g >> 16) & 255, (g >> 8) & 255, g & 255)
    except Exception:  # noqa: BLE001
        gras = (236, 150, 190)
    for k in ("grass_block", "short_grass", "tall_grass", "fern"):
        r.SPECIAL_COLOURS["minecraft:" + k] = gras
    onbekend = set()
    for sid, (bron, turns, cut) in STRUCTUREN.items():
        if only and sid not in only:
            continue
        try:
            st = _tiles(r, bron.rstrip("/")) if bron.endswith("/") else r.load_structure(bron)
            if st is None:
                print("no structure for", sid)
                continue
            if cut:
                # underground: leave out everything above the middle, so you look into the halls
                top = st.size[1] // 2 if cut == "half" else cut
                c = ms.Structure(st.size)
                c.blocks = {k: v for k, v in st.blocks.items() if k[1] <= top}
                c.entities = [e for e in st.entities if e[1] <= top]
                st = c
            _kleuren(r, st, onbekend)
            sprites = _sprites(r, st)
            W, H, D = st.size
            px = max(3, min(18, int(1500 / (W + D))))
            _save(r, r.render_structure(r.turned(st, turns), sprites, px=px, max_size=1500), f"structure_{sid}")
        except Exception as e:  # noqa: BLE001
            print("no render for structure", sid, repr(e))
    if onbekend:
        print("blocks without a known colour (vanilla: drawn grey; our own marker blocks: left out):", ", ".join(sorted(onbekend)))


def renders(r, only=None):
    if only is None or "npcs" in only:
        npcs(r)
    if only is None or "wezens" in only:
        creatures(r)
    if only is None or "blokken" in only:
        blocks(r)
    if only is None or "bouwwerken" in only:
        structures(r)
