"""
bbq2 (F3 wereld): the world helpers. Java: world (GegarandeerdPlacement "alleen_nieuw"/"rond", NieuwTerrein, BouwCheck) and
feature/wereld (Bezetting, Bescherming, QuestRol, Herstel, Kopieen).

The helper library for the slices (from features import wereld):

  wereld.bbq_structuur(h, name, *, soort, titel, tooltip, biomes, salt, ...)
      One call for a new structure of this update: its structure JSON, start pool, structure set(s), biome tag, voorrang
      and Superkompas texts. See the function for every argument.
  wereld.BBQ
      The five biomes of the Guhbarbecuether.
  wereld.npc(h, kind, id, plek=None, yaw=0.0)
      The entity NBT of a quest NPC for a template (Structure.entity), with the Bezetting tag: put the same NPC in the
      template that Bezetting.npc("<id>", ...) registers in Java, so new copies have it from worldgen and the Java side only
      repairs (and brings it to the copies that were generated before this update).
  wereld.ftb_questlijn(fq, module, lijn, stappen, na=(), ...)
      The FTB quests of a building questline: one per step of its Verhaallijn.
  wereld.wiki_questlijn(lijn, titel, lead, structuur, npcs, ftb, ...)
      Its entry for <module>_wiki.py.

  build(h)  the text gui.guhs.wereld.beschermd, the block tag guhs:wereld/natuurlijk (the ground a prop may replace, see
            Bezetting.java) and the game test templates wereld_test_*.
"""
import json
import os

# the biomes of the Guhbarbecuether (features/barbecuether.py BIOMES)
BBQ = ["houtskoolvlakte", "asdal", "satebos", "worstenwoud", "rookdelta"]
SECTOREN = 16                 # the ring of guaranteed copies of the Barbecuether is cut into 16 slices (CONTRACT_130 3)
GEGARANDEERD_SALT = 7         # a guaranteed set's salt = its normal set's salt + 7 (like features/plaatsing.py)

# everything bbq_structuur made in this run: name -> dict(soort, kompas, gegarandeerd, voorrang); also written to
# data/guhs/wereld/bbq_structuren.json for the cross-checks of the merge step (every structure has a Superkompas entry or a sluier)
STRUCTUREN = {}

# the natural ground of our dimensions (worldgen/noise_settings of the Barbecuether and the Guhmensie) and of plain land:
# what a prop of Bezetting.blokken may replace in its bottom layer
NATUURLIJK = ["guhs:houtskoolsteen", "guhs:as_aarde", "guhs:as_blok", "guhs:mosterd_blok", "guhs:mosterd_nylium", "guhs:pindasaus_nylium",
              "guhs:sate_vlees", "guhs:knuffelgras", "guhs:rijpgras", "guhs:vadsmos", "guhs:bleekmos", "guhs:kaasmodder",
              "guhs:modderig_kaasgras", "minecraft:pink_wool", "minecraft:magenta_wool", "minecraft:white_wool",
              "minecraft:pink_terracotta", "minecraft:magenta_terracotta", "minecraft:pink_concrete_powder", "minecraft:grass_block",
              "minecraft:sand", "minecraft:sandstone", "minecraft:clay", "minecraft:snow_block", "minecraft:snow", "#minecraft:dirt"]

LANG = {
    "gui.guhs.wereld.beschermd": "Dit hoort bij het gebouw: hier kun je niks stukmaken of neerzetten. Njeg!",
}


# =====================================================================================================================
# the helper library
# =====================================================================================================================
def bbq_structuur(h, name, *, soort, titel, tooltip, biomes, salt, templates=None, spacing=None, separation=None,
                  gegarandeerd=None, voorrang=None, kompas="barbecue", grootte=None, vlak=None, burcht=None,
                  hoogte=10, grond=0, reach=80, midden=None, spawns=None, step="surface_structures"):
    """A new structure set of this update, with everything around it.

    soort "grot":   type guhs:barbecueput: ONE template on a cave floor of the Guhbarbecuether (never in the sauce sea; in
                    the Guhmensie the same type stands on flat land). templates = [(template, weight)]: each has the centre
                    jigsaw "guhs:<name>_midden" in its ground layer (the jigsaw's layer becomes the top block of the floor).
                    grootte = check_radius (the floor is checked at +-grootte/2 around the middle), vlak =
                    max_height_difference (the corners may differ vlak/2), hoogte = headroom (free blocks above the middle).
    soort "burcht": type guhs:burcht: one big build saved as tiles guhs:<name>/stuk_<i>_<j> (features/spiesburcht_burcht.py
                    save_tiles). burcht = dict(placement="brug"|"paleis", tiles_x, tiles_z, tile_size, anchor, min_y, max_y,
                    reach): "brug" = the deck at the most open height between min_y and max_y, "paleis" = the ground floor
                    at min_y just above the sauce sea. spawns = [(entity, weight, min, max)] (monster spawn overrides).
    soort "land":   a surface building of the Guhmensie (type guhs:flat_jigsaw, like h.structure). templates =
                    [(template, weight)]; grootte = the largest side of the template (flatness check radius), vlak = the
                    height difference it accepts (default 6 + grootte // 4), grond = the template y of the ground layer
                    (the building is sunk that far), reach = max_distance_from_center, midden = the centre jigsaw name
                    ("guhs:<name>_midden") when the template has one.
    biomes:         biome ids (wereld.BBQ, or some of them); written to the tag guhs:has_structure/<name>.
    salt:           the salt of the normal set (your slice's range, ending in 1, 11, 21...); the guaranteed set gets salt + 7.
    spacing, separation: the random spread (chunks); None = no normal set, only the guaranteed copy.
    gegarandeerd:   None | dict(sector=i, min=250, max=900): exactly one copy per world in slice i of 16 of that ring around
                    0,0 | dict(rond="<structure>", min=250, max=500): one copy in that ring around the guaranteed copy of
                    another structure (which must have been declared before this one, with a HIGHER voorrang). Always
                    "alleen_nieuw": in a world that exists already the copy only lands where nothing was generated yet.
    voorrang:       who goes first when two buildings would touch (higher first; default: its reach); h.VOORRANG[name].
    kompas:         the Superkompas tab its name and tooltip are written for; the Java side adds the entry itself:
                    SuperkompasItem.voegToe("<kompas>", "<name>") in your Feature.register. None: not in the Superkompas
                    (a story structure behind a sluier).

    Writes worldgen/structure/<name>.json, worldgen/template_pool/<name>/start.json (not for a burcht),
    worldgen/structure_set/<name>.json and/or <name>_gegarandeerd.json, tags/worldgen/biome/has_structure/<name>.json and
    the texts structure.guhs.<name> (+ .tooltip). make_v2.bouwruimte() fills in keep_clear, voorrang and the step afterwards.
    """
    D = h.D
    assert soort in ("grot", "burcht", "land"), soort
    assert spacing is not None or gegarandeerd is not None, f"{name}: no normal set and no guaranteed copy"
    biome_tag = f"#guhs:has_structure/{name}"
    if soort == "burcht":
        b = dict(burcht)
        h.w(f"{D}/worldgen/structure/{name}.json", {
            "type": "guhs:burcht", "biomes": biome_tag, "step": step, "terrain_adaptation": "none",
            "spawn_overrides": {"monster": {"bounding_box": "piece", "spawns": [
                {"type": t, "weight": wgt, "minCount": lo, "maxCount": hi} for t, wgt, lo, hi in spawns]}} if spawns else {},
            "templates": f"guhs:{name}", "tiles_x": b["tiles_x"], "tiles_z": b["tiles_z"], "tile_size": b["tile_size"],
            "anchor": list(b["anchor"]), "placement": b["placement"], "min_y": b["min_y"], "max_y": b["max_y"],
            "reach": b.get("reach", 32)})
    else:
        assert templates, f"{name}: templates=[(template, weight), ...]"
        pool = {"fallback": "minecraft:empty", "elements": [
            {"weight": wgt, "element": {"element_type": "minecraft:single_pool_element", "location": f"guhs:{t}",
                                        "projection": "rigid", "processors": "minecraft:empty"}} for t, wgt in templates]}
        if soort == "grot":
            h.w(f"{D}/worldgen/structure/{name}.json", {
                "type": "guhs:barbecueput", "biomes": biome_tag, "step": step, "spawn_overrides": {},
                "terrain_adaptation": "beard_thin", "start_pool": f"guhs:{name}/start", "start_jigsaw_name": f"guhs:{name}_midden",
                "check_radius": grootte if grootte is not None else 16,
                "max_height_difference": vlak if vlak is not None else 8, "headroom": hoogte})
        else:
            radius = grootte if grootte is not None else 16
            h.TEMPLATE_SIZES[name] = radius
            if vlak is not None:
                h.FLATNESS[name] = vlak
            jigsaw = {"type": "minecraft:jigsaw", "biomes": biome_tag, "step": step, "spawn_overrides": {},
                      "terrain_adaptation": "beard_box" if reach <= 116 else "none", "start_pool": f"guhs:{name}/start", "size": 1,
                      "start_height": {"absolute": -grond}, "project_start_to_heightmap": "WORLD_SURFACE_WG",
                      "max_distance_from_center": reach, "use_expansion_hack": False}
            if midden:
                jigsaw["start_jigsaw_name"] = midden
            h.w(f"{D}/worldgen/structure/{name}.json", {
                "type": "guhs:flat_jigsaw", "biomes": biome_tag, "step": step, "spawn_overrides": {},
                "terrain_adaptation": "beard_box", "check_radius": radius,
                "max_height_difference": h.FLATNESS.get(name, 6 + radius // 4), "keep_clear": (radius + 1) // 2, "jigsaw": jigsaw})
        h.w(f"{D}/worldgen/template_pool/{name}/start.json", pool)
    # the sets: the normal one (random spread) and/or the guaranteed copy
    entry = [{"structure": f"guhs:{name}", "weight": 1}]
    normal = f"{D}/worldgen/structure_set/{name}.json"
    if spacing is not None:
        assert separation is not None and separation < spacing, f"{name}: separation must be smaller than spacing"
        h.w(normal, {"structures": entry, "placement": {"type": "minecraft:random_spread", "spacing": spacing,
                                                        "separation": separation, "salt": salt}})
    elif os.path.exists(normal):
        os.remove(normal)   # (an earlier run had a normal set)
    guaranteed = f"{D}/worldgen/structure_set/{name}_gegarandeerd.json"
    if gegarandeerd is not None:
        g = dict(gegarandeerd)
        placement = {"type": "guhs:gegarandeerd", "salt": salt + GEGARANDEERD_SALT, "min_afstand": g.get("min", 250),
                     "max_afstand": g.get("max", 900), "alleen_nieuw": True}
        if "rond" in g:
            rond = g["rond"]
            if rond not in STRUCTUREN or STRUCTUREN[rond]["gegarandeerd"] is None:
                raise SystemExit(f"wereld.bbq_structuur({name}): rond={rond!r} must be declared first, with a guaranteed copy")
            if voorrang is None or STRUCTUREN[rond]["voorrang"] is None or STRUCTUREN[rond]["voorrang"] <= voorrang:
                raise SystemExit(f"wereld.bbq_structuur({name}): rond={rond!r} must go first: give both a voorrang, {rond} the higher one "
                                 f"({rond}: {STRUCTUREN[rond]['voorrang']}, {name}: {voorrang})")
            placement.update(sector=0, sectoren=1, rond=f"guhs:{rond}_gegarandeerd")
        else:
            assert 0 <= g["sector"] < SECTOREN, f"{name}: sector 0..{SECTOREN - 1}"
            placement.update(sector=g["sector"], sectoren=SECTOREN)
        h.w(guaranteed, {"structures": entry, "placement": placement})
    elif os.path.exists(guaranteed):
        os.remove(guaranteed)
    h.w(f"{D}/tags/worldgen/biome/has_structure/{name}.json", {"values": [b if ":" in b else f"guhs:{b}" for b in biomes]})
    if voorrang is not None:
        h.VOORRANG[name] = voorrang
    else:
        h.VOORRANG.pop(name, None)
    h.lang(f"structure.guhs.{name}", titel, titel)
    h.lang(f"structure.guhs.{name}.tooltip", tooltip, tooltip)
    STRUCTUREN[name] = dict(soort=soort, kompas=kompas, gegarandeerd=gegarandeerd, voorrang=voorrang, salt=salt,
                            spacing=spacing, biomes=list(biomes))
    _schrijf_overzicht(h)


def _schrijf_overzicht(h):
    """data/guhs/wereld/bbq_structuren.json: {tab or "sluier": [structures]} (for the merge step's cross-checks)."""
    per = {}
    for name, s in STRUCTUREN.items():
        per.setdefault(s["kompas"] or "sluier", []).append(name)
    h.w(f"{h.D}/wereld/bbq_structuren.json", {k: sorted(v) for k, v in sorted(per.items())})


def npc(h, kind, id, plek=None, yaw=0.0):
    """The entity NBT of a quest NPC in a template: s.entity(x + 0.5, y, z + 0.5, wereld.npc(h, "wachterguh", "bestaand_wachter")).
    `id` is the id of Bezetting.npc(...) in Java (its tag), `plek` the NpcRollen plek (RoleData guhs_plek) when the kind has
    more than one role."""
    nbt = {"id": "guhs:guh_npc", "Kind": kind, "PersistenceRequired": h.Byte(1), "Invulnerable": h.Byte(1),
           "Rotation": h.floats(float(yaw), 0.0), "NeoForgeData": {"guhs_bezetting": id}}
    if plek:
        nbt["RoleData"] = {"guhs_plek": plek}
    return nbt


def ftb_questlijn(fq, module, lijn, stappen, na=(), beloning=(("guhs:kaas_knabbels", 8),), eind=None):
    """The FTB quests of a questline of a building: one per step of the Verhaallijn `lijn` (call it from your ftb(fq)).
    stappen = [(titel, tekst, icon), ...] for the steps 1..n: quest key "<module>_<lijn>_<i>", task fq.adv("<lijn>_stap_<i>")
    (the hidden advancement the Verhaallijn grants when the player reaches step i). Each quest comes after the one before
    (only a line in the book, unless the module has FTB_LINEAIR); `na`: what the first one comes after. `eind`: the rewards
    of the last quest (default: `beloning`), which is drawn as a gear with some xp. Returns the quest keys."""
    keys = []
    for i, (titel, tekst, icon) in enumerate(stappen, start=1):
        key = f"{module}_{lijn}_{i}"
        laatste = i == len(stappen)
        fq.q(key, titel, tekst, icon, [fq.adv(f"{lijn}_stap_{i}")], rewards=tuple(eind) if laatste and eind else tuple(beloning),
             deps=[keys[-1]] if keys else list(na), shape="gear" if laatste else None, xp=100 if laatste else 0)
        keys.append(key)
    return keys


def wiki_questlijn(lijn, titel, lead, structuur, npcs, ftb, img=None, related=()):
    """The entry of a building's questline for your <module>_wiki.py: WIKI = {"verhalen": dict([wereld.wiki_questlijn(...)]),
    "npc_home": {...}}. lijn = the page slug (the Verhaallijn id), lead = one Dutch paragraph, structuur = the structure id,
    npcs = the NPC kinds, ftb = [(chapter, section id)]. Returns (slug, dict) in the shape of CONTRACT_130 2.4."""
    return lijn, dict(nl=titel, img=img or f"structure_{structuur}", lead_nl=lead, ftb=list(ftb), structure=structuur, npcs=list(npcs),
                      related=list(related))


# =====================================================================================================================
# this module's own resources
# =====================================================================================================================
def texts(h):
    for key, text in LANG.items():
        h.lang(key, text, text)


def tags(h):
    h.add_tag("guhs/tags/block/wereld/natuurlijk", NATUURLIJK)


def test_templates(h):
    """WereldGameTests: an empty room with a floor of houtskoolsteen, a little building (a floor, four posts and an NPC that
    came with the template) and a prop (a tent of two blocks with a lantern)."""
    kamer = h.Structure((24, 8, 24))
    for x in range(24):
        for z in range(24):
            kamer.set(x, 0, z, "guhs:houtskoolsteen")
    kamer.save("wereld_test_kamer")
    gebouw = h.Structure((7, 5, 7))
    for x in range(7):
        for z in range(7):
            gebouw.set(x, 0, z, "minecraft:polished_blackstone_bricks")
    for x, z in ((0, 0), (6, 0), (0, 6), (6, 6)):
        for y in range(1, 4):
            gebouw.set(x, y, z, "minecraft:polished_blackstone_wall",
                       {"up": "true", "north": "none", "south": "none", "east": "none", "west": "none", "waterlogged": "false"})
    gebouw.entity(3.5, 1.0, 3.5, npc(h, "wachterguh", "wereld_test_sjabloon"))
    gebouw.save("wereld_test_gebouw")
    prop = h.Structure((3, 3, 3))
    for x in range(3):
        for z in range(3):
            prop.set(x, 0, z, "minecraft:red_wool")
    prop.set(1, 1, 1, "minecraft:lantern", {"hanging": "false", "waterlogged": "false"})
    prop.save("wereld_test_prop")


def selfcheck(h):
    problems = []
    tag = json.load(open(f"{h.R}/data/guhs/tags/block/wereld/natuurlijk.json", encoding="utf-8"))
    for b in NATUURLIJK:
        if b not in tag["values"]:
            problems.append(f"tag guhs:wereld/natuurlijk misses {b}")
    for name in ("wereld_test_kamer", "wereld_test_gebouw", "wereld_test_prop"):
        if not os.path.exists(f"{h.D}/structure/{name}.nbt"):
            problems.append(f"template {name}")
    if problems:
        raise SystemExit("wereld self-check failed:\n  " + "\n  ".join(problems))


def build(h):
    STRUCTUREN.clear()
    texts(h)
    tags(h)
    test_templates(h)
    selfcheck(h)
