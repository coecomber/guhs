"""
Shared helpers for the guhpixel slice generators (tools/features/guhpixel_<slice>.py). Java side: the building blocks in
feature/guhpixel/blok (DecoBlock, MuurDecoBlock, LoreBlockItem).

  deco(h, id, elements, textures, naam, lore)        a floor decoration (DecoBlock): model, 4 facings, item, loot, texts
  muurdeco(h, id, elements, textures, naam, lore)    a wall decoration (MuurDecoBlock): the same; model given for facing north
                                                      (so it lies against the south side of its block: z 14..16)
  npc(h, kind, naam, hue, sat=0.75, val=1.0)         a lobby/NPC kind: its name and a recoloured sitting guh npc_<kind>.png
  geluid(h, {event: [entries]}, {event: subtitle})   sounds.json entries (reusing existing sounds) + their subtitles
  quest_adv(h, name)                                 a hidden advancement quest/<name> (granted with GuhAdvancements.grant)
  test_kamer(h, name, size, vloer=...)               a game test room: a floor only
  teksten(h, {key: Dutch})                           h.lang for a whole dict (Dutch is the source; English: tools/lang/en)
  controleer(h, naam, blokken=(), items=(), keys=()) the usual self-check: blockstate, model, item model, loot, lang keys
  kiosk(h, name, spacing, separation, salt, sector, ring, portaal)
                                                      a PLACEHOLDER worldgen structure (13 x 10 x 13) with its random_spread set
                                                      and its guaranteed copy; the owning slice replaces it

Every text is Dutch; the English goes in the slice's own chunk file tools/lang/en/c3x_px_<slice>.json.
"""
import os

from features import uvfix

ROT = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def teksten(h, tabel):
    for key, nl in tabel.items():
        if "te vads" in nl.lower() or "hamster" in nl.lower():
            raise SystemExit(f"guhpixel text check failed: {key}: {nl}")
        h.lang(key, nl, nl)


def _blok(h, bid, elements, textures, naam, lore, extra_model=None):
    particle = textures.get("particle") or next(iter(textures.values()))
    model = {"parent": "minecraft:block/block", "textures": {"particle": particle, **textures},
             "elements": uvfix.binnen(elements)}
    if extra_model:
        model.update(extra_model)
    h.w(f"{h.A}/models/block/{bid}.json", model)
    h.w(f"{h.A}/blockstates/{bid}.json", {"variants": h.facing_states(bid)})
    h.w(f"{h.A}/models/item/{bid}.json", {"parent": f"guhs:block/{bid}"})
    h.self_drop(bid)
    h.lang(f"block.guhs.{bid}", naam, naam)
    if lore:
        h.lang(f"block.guhs.{bid}.lore", lore, lore)


def deco(h, bid, elements, textures, naam, lore=None, display=None):
    """A floor decoration block (Java: DecoBlock + LoreBlockItem). elements/textures: a normal block model, facing north."""
    _blok(h, bid, elements, textures, naam, lore, {"display": display} if display else None)


def muurdeco(h, bid, elements, textures, naam, lore=None, display=None):
    """A wall decoration block (Java: MuurDecoBlock + LoreBlockItem). The model hangs against the south side (z 14..16)."""
    _blok(h, bid, elements, textures, naam, lore, {"display": display or {
        "gui": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, -7], "scale": [1, 1, 1]},
        "ground": {"rotation": [0, 180, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]}}})


def npc(h, kind, naam, hue, sat=0.75, val=1.0):
    """An NPC kind (GuhNpcEntity.Kind, lower case): entity.guhs.guh_npc.<kind> and the texture npc_<kind>.png (a recoloured
    sitting guh; replace the file afterwards for a drawn one)."""
    h.lang(f"entity.guhs.guh_npc.{kind}", naam, naam)
    src = h.Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    img = h.recolour(src, hue=hue, sat=sat, val=val, only=h.pinkish).convert("RGBA")
    h.save(img, "entity", f"npc_{kind}.png")


def geluid(h, events, ondertitels=None):
    """events: {"n.what": [{"name": "guhs:guh_ambient5", "pitch": 1.5}, {"name": "minecraft:block.note_block.bit", "type": "event"}]};
    ondertitels: {"n.what": "Dutch subtitle"} (every event needs one). Java: SoundEvent.createVariableRangeEvent(Guhs.id("n.what"))."""
    def patch(d):
        for event, entries in events.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)
    for event, tekst in (ondertitels or {}).items():
        h.lang(f"subtitles.guhs.{event}", tekst, tekst)
    missing = [e for e in events if f"subtitles.guhs.{e}" not in h.NL]
    if missing:
        raise SystemExit("guhpixel sounds without a subtitle: " + ", ".join(missing))


def quest_adv(h, name):
    """A hidden advancement guhs:quest/<name> (no display): FTB tasks check it with fq.adv("<name>")."""
    h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


def test_kamer(h, name, size, vloer=("minecraft:pink_concrete", "minecraft:white_concrete")):
    """A game test room data/guhs/structure/<name>.nbt: a checkered floor on y 0, air above. Returns the Structure before
    saving is done, so call .save yourself only when you add things: this helper saves it."""
    s = h.Structure(tuple(size))
    for x in range(size[0]):
        for z in range(size[2]):
            s.set(x, 0, z, vloer[(x + z) % 2])
    s.save(name)
    return s


def controleer(h, naam, blokken=(), items=(), keys=(), templates=()):
    """The usual self-check of a slice: raises SystemExit listing what is missing."""
    problems = []
    for b in blokken:
        for path in (f"{h.A}/blockstates/{b}.json", f"{h.A}/models/block/{b}.json"):
            if not os.path.exists(path):
                problems.append(f"missing {path}")
        if f"block.guhs.{b}" not in h.NL:
            problems.append(f"no name for block {b}")
    for i in items:
        if not os.path.exists(f"{h.A}/models/item/{i}.json"):
            problems.append(f"no item model for {i}")
        if f"item.guhs.{i}" not in h.NL and f"block.guhs.{i}" not in h.NL:
            problems.append(f"no name for item {i}")
    for k in keys:
        if k not in h.NL:
            problems.append(f"missing lang key {k}")
    for t in templates:
        if not os.path.exists(f"{h.D}/structure/{t}.nbt"):
            problems.append(f"missing template {t}")
    if problems:
        raise SystemExit(f"{naam} self-check failed:\n  " + "\n  ".join(problems))


def gegarandeerd(h, name, salt, sector, ring, sectoren=2):
    """The guaranteed copy of a structure: one in the ring (min, max blocks from 0,0), in its own sector. The existing rings
    (700-2500) lie inside the live server's pregenerated area, so the guhpixel structures use 4300-5600. Written here and not
    in plaatsing.py (its lists are asserted by PlaatsingGameTests)."""
    h.w(f"{h.D}/worldgen/structure_set/{name}_gegarandeerd.json", {
        "structures": [{"structure": f"guhs:{name}", "weight": 1}],
        "placement": {"type": "guhs:gegarandeerd", "salt": salt + 7, "min_afstand": ring[0], "max_afstand": ring[1],
                      "sector": sector, "sectoren": sectoren}})


def kiosk(h, name, spacing, separation, salt, sector, ring, portaal=False):
    """A placeholder structure (13 x 10 x 13) so /locate, the Superkompas and the FTB structure task work before the slice
    builds the real one: a little stand on a pink floor; with portaal a 3 x 3 screen of guhs:guhpixel_portaal[soort=in]."""
    G, W = 4, 13
    s = h.Structure((W, 10, W))
    for x in range(W):
        for z in range(W):
            for y in range(G):
                s.set(x, y, z, "minecraft:pink_wool")
            s.set(x, G, z, "guhs:knuffelklinkers" if abs(x - 6) <= 1 or abs(z - 6) <= 1 else "guhs:knuffelgras")
    for (x, z) in ((3, 3), (9, 3), (3, 9), (9, 9)):
        for y in range(G + 1, G + 6):
            s.set(x, y, z, "minecraft:white_concrete")
    for x in range(2, 11):
        for z in range(2, 11):
            s.set(x, G + 6, z, "minecraft:yellow_concrete" if (x + z) % 2 else "minecraft:pink_concrete")
    if portaal:
        # a big beige "monitor" with the screen in it, open to the south (z+) and the north
        for x in range(4, 9):
            for y in range(G + 1, G + 6):
                rand = x in (4, 8) or y in (G + 1, G + 5)
                if rand:
                    s.set(x, y, 6, "minecraft:smooth_sandstone")
                else:
                    s.set(x, y, 6, "guhs:guhpixel_portaal", {"soort": "in"})
        s.set(6, G + 1, 6, "guhs:guhpixel_portaal", {"soort": "in"})   # (a doorway: walk in at floor height)
    anchor = f"guhs:{name}_midden"
    s.set(6, G, 6, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": anchor, "target": "minecraft:empty", "pool": "minecraft:empty", "final_state": "guhs:knuffelklinkers",
           "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    s.clear_above([(x, z) for x in range(W) for z in range(W)], G + 1, top=10)
    none = {"bounding_box": "piece", "spawns": []}
    h.TEMPLATE_SIZES[name] = W
    h.structure(name, h.GUHMENSION_LAND, spacing=spacing, separation=separation, salt=salt, start_y=-G, reach=40, centre=anchor,
                spawn_overrides={"monster": none})
    s.save(name)
    gegarandeerd(h, name, salt, sector, ring)
    return s
