"""
Placeholders for the phase-2 features of 2.8 (made by phase 1, the knuffeldal slice), used by the stub generators
tools/features/<pkg>.py until each slice replaces its stub:

  npc(h, kind, name, rarity, info, hue)      NPC name, Guhdex page texts, a recoloured sitting guh npc_<kind>.png,
                                             and the hidden advancement quest/seen_<kind>
  creature(h, page, name, rarity, info)     a creature page (no NPC kind): entity name, page texts, quest/seen_<page>
  loose(h, name, biomes, spacing, sep, salt, kind, title, tooltip)
                                             a loose structure (make_v2 h.structure, centre jigsaw guhs:<name>_midden)
                                             with a small placeholder template: a little stand with the NPC
  slot(h, slot, kind, title)                 a plein slot template (31 x 16 x 31, jigsaw (15, 4, 30) south_up
                                             guhs:plein_ingang): a building site with a kiosk and the NPC
  grijpmachine(h)                            the grijpmachine slot (5 x 6 x 5, jigsaw (2, 0, 2) down_south)
Each template gets a small geometry self-check (SystemExit on problems).
"""
import os

G = 4
SLOT = (31, 16, 31)
KS = "guhs:knuffelsteen"
KLINK = "guhs:knuffelklinkers"


def npc(h, kind, name, rarity, info, hue, sat=0.75, val=1.0):
    h.lang(f"entity.guhs.guh_npc.{kind}", name, name)
    h.lang(f"gui.guhs.guhdex.rarity.{kind}", "Zeldzaamheid: " + rarity, "Zeldzaamheid: " + rarity)
    h.lang(f"gui.guhs.guhdex.info.{kind}", info, info)
    h.w(f"{h.D}/advancement/quest/seen_{kind}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    src = h.Image.open(os.path.join(h.TEX, "entity", "guh_sitting.png"))
    img = h.recolour(src, hue=hue, sat=sat, val=val, only=h.pinkish).convert("RGBA")
    h.save(img, "entity", f"npc_{kind}.png")


def creature(h, page, name, rarity, info):
    h.lang(f"entity.guhs.{page}", name, name)
    h.lang(f"gui.guhs.guhdex.rarity.{page}", "Zeldzaamheid: " + rarity, "Zeldzaamheid: " + rarity)
    h.lang(f"gui.guhs.guhdex.info.{page}", info, info)
    h.w(f"{h.D}/advancement/quest/seen_{page}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


def _check(s, name, npc_spot, jig, floor_y):
    problems = []
    x, y, z = npc_spot
    if s.get(x, y - 1, z) in (None, "minecraft:air") or s.get(x, y, z) not in (None, "minecraft:air"):
        problems.append(f"{name}: the NPC at {npc_spot} doesn't stand on a floor")
    if s.get(*jig) != "minecraft:jigsaw":
        problems.append(f"{name}: no jigsaw at {jig}")
    for (px, py, pz), (b, _, _) in s.blocks.items():
        if b != "minecraft:air" and py > floor_y and all(s.get(px + dx, py + dy, pz + dz) in (None, "minecraft:air")
                                                         for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))):
            problems.append(f"{name}: a floating {b} at {(px, py, pz)}")
    if problems:
        raise SystemExit("placeholder check failed:\n  " + "\n  ".join(problems))


def _kiosk(h, s, cx, cz, kind, facing_yaw):
    """A little kiosk: knuffelsteen posts, a pluisdak roof, a guh face on top, the NPC in the middle."""
    for (x, z) in ((cx - 2, cz - 2), (cx + 2, cz - 2), (cx - 2, cz + 2), (cx + 2, cz + 2)):
        for y in range(G + 1, G + 5):
            s.set(x, y, z, "guhs:knuffelsteen_muur", {"up": "true"})
    for x in range(cx - 3, cx + 4):
        for z in range(cz - 3, cz + 4):
            edge = max(abs(x - cx), abs(z - cz))
            s.set(x, G + 5 + (3 - edge) // 2, z, "guhs:pluisdak")
            if edge < 3:
                s.set(x, G + 5, z, "guhs:pluisdak")
    s.set(cx, G + 7, cz, "guhs:knuffelsteen_gezicht", {"facing": "south", "stemming": "0"})
    ms = h.ms
    s.entity(cx + 0.5, float(G + 1), cz + 0.5, {"id": "guhs:guh_npc", "Kind": kind, "PersistenceRequired": ms.Byte(1),
                                                "Rotation": ms.floats(facing_yaw, 0.0)})


def loose(h, name, biomes, spacing, sep, salt, kind, title, tooltip):
    """A loose 2.8 structure: a placeholder template (13 x 10 x 13) with the NPC in a kiosk and the centre jigsaw."""
    W = 13
    s = h.Structure((W, 10, W))
    for x in range(W):
        for z in range(W):
            for y in range(G):
                s.set(x, y, z, "minecraft:pink_wool")
            s.set(x, G, z, KLINK if abs(x - 6) <= 1 or abs(z - 6) <= 1 else "guhs:knuffelgras")
    if kind:
        _kiosk(h, s, 6, 6, kind, 0.0)
    for (x, z) in ((1, 1), (11, 1), (1, 11), (11, 11)):
        s.set(x, G + 1, z, "guhs:seizoensbloembak", {"seizoen": "lente"})
    anchor = f"guhs:{name}_midden"
    s.set(6, G, 6, "minecraft:jigsaw", {"orientation": "up_north"},
          {"id": "minecraft:jigsaw", "name": anchor, "target": "minecraft:empty", "pool": "minecraft:empty", "final_state": KLINK,
           "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    s.clear_above([(x, z) for x in range(W) for z in range(W)], G + 1, top=10)
    _check(s, name, (6, G + 1, 6) if not kind else (6, G + 1, 6), (6, G, 6), G)
    none = {"bounding_box": "piece", "spawns": []}
    h.TEMPLATE_SIZES[name] = W
    h.structure(name, biomes, spacing=spacing, separation=sep, salt=salt, start_y=-G, reach=40, centre=anchor,
                spawn_overrides={"monster": none})
    s.save(name)
    h.lang(f"structure.guhs.{name}", title, title)
    h.lang(f"structure.guhs.{name}.tooltip", tooltip, tooltip)


def slot(h, slotname, kind, title):
    """A plein slot placeholder (31 x 16 x 31): a building site with a kiosk, the NPC in it, flower boxes."""
    W, H, D = SLOT
    s = h.Structure(SLOT)
    for x in range(W):
        for z in range(D):
            for y in range(G):
                s.set(x, y, z, "minecraft:pink_wool")
            s.set(x, G, z, KLINK if 13 <= x <= 17 else "guhs:knuffelgras")
    # a fence of knuffelsteen walls around the building site, open at the front
    for x in range(2, W - 2):
        for z in (2, D - 3):
            if not (z == D - 3 and 12 <= x <= 18):
                s.set(x, G + 1, z, "guhs:knuffelsteen_muur", {"up": "true" if x % 4 == 0 else "false", "east": "low", "west": "low"})
    for z in range(3, D - 3):
        for x in (2, W - 3):
            s.set(x, G + 1, z, "guhs:knuffelsteen_muur", {"up": "true" if z % 4 == 0 else "false", "north": "low", "south": "low"})
    _kiosk(h, s, 15, 15, kind, 0.0)
    for (x, z) in ((6, 8), (24, 8), (6, 22), (24, 22)):
        s.set(x, G + 1, z, "guhs:seizoensbloembak", {"seizoen": "lente"})
    # piles of building stuff (a building site!)
    for (x, z) in ((5, 5), (6, 5), (5, 6), (25, 5)):
        s.set(x, G + 1, z, KS)
    s.set(5, G + 2, 5, "guhs:pluisdak")
    s.set(15, G, 30, "minecraft:jigsaw", {"orientation": "south_up"},
          {"id": "minecraft:jigsaw", "name": "guhs:plein_ingang", "target": "minecraft:empty", "pool": "minecraft:empty", "final_state": KLINK,
           "joint": "rollable", "placement_priority": 0, "selection_priority": 0})
    for x in (14, 15, 16):
        for y in (G + 1, G + 2, G + 3):
            for z in (28, 29, 30):
                s.blocks.pop((x, y, z), None)
    s.clear_above([(x, z) for x in range(W) for z in range(D)], G + 1, top=H)
    _check(s, f"knuffeldal_stadje/{slotname}", (15, G + 1, 15), (15, G, 30), G)
    s.save(f"knuffeldal_stadje/{slotname}")
    h.lang(f"gui.guhs.knus.slot.{slotname}", title, title)


def grijpmachine(h):
    """The grijpmachine slot placeholder (5 x 6 x 5): a little pink cabinet on a pedestal, with a guh face on top."""
    s = h.Structure((5, 6, 5))
    for x in range(1, 4):
        for z in range(1, 4):
            s.set(x, 0, z, KS)
    s.set(2, 0, 2, "minecraft:jigsaw", {"orientation": "down_south"},
          {"id": "minecraft:jigsaw", "name": "guhs:grijpmachine_voet", "target": "minecraft:empty", "pool": "minecraft:empty",
           "final_state": KLINK, "joint": "aligned", "placement_priority": 0, "selection_priority": 0})
    for x in range(1, 4):
        for z in range(1, 4):
            for y in (1, 2):
                s.set(x, y, z, "minecraft:pink_concrete")
            s.set(x, 3, z, "minecraft:pink_stained_glass")
            s.set(x, 4, z, "guhs:knuffelsteen_plaat", {"type": "bottom"})
    s.set(2, 5, 2, "guhs:knuffelsteen_gezicht", {"facing": "south", "stemming": "3"})
    s.save("knuffeldal_stadje/grijpmachine")
