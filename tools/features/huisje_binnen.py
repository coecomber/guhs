"""
Huisje betreden (1.3.2): the INSIDE of a Guhhuisje. Called from huisje.py (no own FEATURES entry).

  - the hidden void dimension guhs:huisje_binnen (dimension, dimension type, biome): a soft fixed light, no weather, no mob
    spawning, beds work at any time and never set the spawn point; every huisje has its own room there on a grid
    (Java: feature/huisje/Binnen);
  - one room template per size, data/guhs/structure/huisje_binnen/<maat>.nbt, on guh scale (the ceiling is two blocks high,
    the beds are one block): a guhbedje per resident slot with a name sign above it, a bedside table (the favourite thing)
    and a hook (the clothes) beside it, a prikbord, a logeerbedje for the player, the doormat and the door, a knabbelbak,
    cushions, little windows with a painted view and a pink lamp in the ceiling;
  - data/guhs/huisje_binnen/kamers.json: the template version and, per size, where everything stands (template
    coordinates). Java reads it (BinnenKamer), so the coordinates live HERE only. Raise VERSIE whenever a template changes:
    the rooms that exist on a server are then stamped again the next time somebody goes in;
  - the blocks guhs:huisje_bedje (the guh bed) and guhs:huisje_raam (the painted window), the doorbell sound, the hidden
    advancement quest/huisje_binnen, the texts.
"""
import os

import numpy as np
from PIL import Image, ImageDraw

import sign_text

# Raise this whenever a room template changes (existing rooms are stamped again).
VERSIE = 1

MATEN = ["klein", "medium", "groot"]
# per size: interior width (x) and depth (z), the beds along the north wall, along the west wall and along the east wall
PLAN = {
    "klein": {"w": 7, "d": 5, "noord": 3, "west": 0, "oost": 0},
    "medium": {"w": 7, "d": 6, "noord": 3, "west": 1, "oost": 1},
    "groot": {"w": 9, "d": 8, "noord": 4, "west": 2, "oost": 2},
}
PLEKKEN = {"klein": 3, "medium": 5, "groot": 8}
H = 2   # (the room is two blocks high inside: a guh house, the player feels big)

VLOER, MUUR, PLAFOND = "minecraft:cherry_planks", "minecraft:pink_wool", "minecraft:white_wool"
SIGN = "sign.guhs.huisje_binnen"


def kamer(maat):
    """The layout of one room: (size, {what: where}) in template coordinates. Interior cell (ix, iz) is block (ix + 1, iz + 1);
    the floor is y 0, the room y 1..2, the ceiling y 3; the door is in the south wall."""
    p = PLAN[maat]
    w, d = p["w"], p["d"]
    bedden = []
    for i in range(p["noord"]):          # along the north wall: bedside table, bed, bedside table, bed...
        ix = 1 + 2 * i
        bedden.append({"bed": (ix + 1, 1, 1), "kijk": "south", "bord": (ix + 1, 2, 1), "kastje": (ix, 1, 1), "haak": (ix, 2, 1)})
    for i in range(p["west"]):
        iz = 2 + 2 * i
        bedden.append({"bed": (1, 1, iz + 1), "kijk": "east", "bord": (1, 2, iz + 1), "kastje": (1, 1, iz + 2), "haak": (1, 2, iz + 2)})
    for i in range(p["oost"]):
        iz = 2 + 2 * i
        bedden.append({"bed": (w, 1, iz + 1), "kijk": "west", "bord": (w, 2, iz + 1), "kastje": (w, 1, iz + 2), "haak": (w, 2, iz + 2)})
    assert len(bedden) == PLEKKEN[maat], maat
    deur_x = (w - 1) // 2 + 1
    return {
        "maat": (w + 2, H + 2, d + 3),
        "mat": (deur_x, 1, d),
        "yaw": 180.0,
        "deur": [(deur_x, 1, d + 1), (deur_x, 2, d + 1)],
        "prikbord": (w - 1, 2, d),
        "logeerbed": {"hoofd": (1, 1, d), "voet": (2, 1, d)},
        "bak": (w, 1, 1),
        "bedden": bedden,
    }


def template(h, maat):
    k = kamer(maat)
    p = PLAN[maat]
    w, d = p["w"], p["d"]
    X, Y, Z = k["maat"]
    s = h.Structure((X, Y, Z))
    # the shell: floor, walls, ceiling (the extra row at the south is the wool behind the door)
    for x in range(X):
        for z in range(d + 2):
            s.set(x, 0, z, VLOER)
            s.set(x, H + 1, z, PLAFOND)
            rand = x in (0, X - 1) or z in (0, d + 1)
            for y in range(1, H + 1):
                s.set(x, y, z, MUUR if rand else "minecraft:air")
    for y in range(0, H + 2):
        for x in (k["deur"][0][0] - 1, k["deur"][0][0], k["deur"][0][0] + 1):
            s.set(x, y, d + 2, MUUR)
    # a soft rug in the middle
    for x in range(3, w - 1):
        for z in range(3, d - 0):
            s.set(x, 1, z, "minecraft:pink_carpet" if (x + z) % 2 else "minecraft:white_carpet")
    # the beds, each with its name sign, bedside table and hook
    for b in k["bedden"]:
        kop = {"south": "north", "east": "west", "west": "east"}[b["kijk"]]   # (the pillow lies against the wall)
        s.set(*b["bed"], "guhs:huisje_bedje", {"facing": kop})
        s.set(*b["bord"], "minecraft:cherry_wall_sign", {"facing": b["kijk"], "waterlogged": "false"})
        s.set(*b["kastje"], "minecraft:cherry_slab", {"type": "bottom", "waterlogged": "false"})
        s.set(*b["haak"], "minecraft:tripwire_hook", {"facing": b["kijk"], "attached": "false", "powered": "false"})
    # the door (its panel on the inside of the wall) with the doormat in front of it
    dx = k["deur"][0][0]
    for y, half in ((1, "lower"), (2, "upper")):
        s.set(dx, y, d + 1, "minecraft:cherry_door", {"facing": "south", "half": half, "hinge": "left", "open": "false", "powered": "false"})
    s.set(*k["mat"], "minecraft:magenta_carpet")
    # the logeerbedje (a real bed: the player sleeps in it) along the south wall
    s.set(*k["logeerbed"]["hoofd"], "minecraft:pink_bed", {"facing": "west", "part": "head", "occupied": "false"})
    s.set(*k["logeerbed"]["voet"], "minecraft:pink_bed", {"facing": "west", "part": "foot", "occupied": "false"})
    # the prikbord on the south wall
    tekst = {"messages": sign_text.messages(SIGN, ["~ Prikbord ~", "Wie doet", "welk klusje?", "(klik, njeg)"]), "color": "black",
             "has_glowing_text": h.ms.Byte(0)}
    leeg = {"messages": sign_text.messages(SIGN, ["", "", "", ""]), "color": "black", "has_glowing_text": h.ms.Byte(0)}
    s.set(*k["prikbord"], "minecraft:cherry_wall_sign", {"facing": "north", "waterlogged": "false"},
          {"id": "minecraft:sign", "is_waxed": h.ms.Byte(1), "front_text": tekst, "back_text": leeg})
    # the knabbelbak in the corner, cushions where there is room
    s.set(*k["bak"], "guhs:knabbelbak", {"facing": "south"})
    s.set(w, 1, d, "guhs:pink_kussen", {"facing": "north"})
    if maat != "klein":
        s.set(w - 2, 1, d - 1, "guhs:magenta_kussen", {"facing": "west"})
    # little windows with a painted view: in the side walls and next to the door (never behind a sign or a hook)
    ramen = [(0, 2, d - 1), (X - 1, 2, d - 1), (dx - 2, 2, d + 1), (dx + 2, 2, d + 1)]
    if p["west"] == 0:
        ramen += [(0, 2, 3), (X - 1, 2, 3)]
    achter_prikbord = (k["prikbord"][0], 2, d + 1)
    for r in ramen:
        if s.get(*r) == MUUR and r != achter_prikbord:
            s.set(*r, "guhs:huisje_raam")
    # the pink lamp(s) in the ceiling
    for x in ((X // 2,) if w <= 7 else (X // 2 - 2, X // 2 + 2)):
        for z in ((1 + d // 2,) if d <= 6 else (3, d - 1)):
            s.set(x, H + 1, z, "minecraft:pearlescent_froglight", {"axis": "y"})
    s.save(f"huisje_binnen/{maat}")
    return k


def kamers(h):
    uit = {"versie": VERSIE, "kamers": {}}
    for maat in MATEN:
        k = template(h, maat)
        uit["kamers"][maat] = {
            "maat": list(k["maat"]), "mat": list(k["mat"]), "yaw": k["yaw"], "deur": [list(p) for p in k["deur"]],
            "prikbord": list(k["prikbord"]),
            "logeerbed": {"hoofd": list(k["logeerbed"]["hoofd"]), "voet": list(k["logeerbed"]["voet"])},
            "bedden": [{"bed": list(b["bed"]), "kijk": b["kijk"], "bord": list(b["bord"]), "kastje": list(b["kastje"]),
                        "haak": list(b["haak"])} for b in k["bedden"]]}
    h.w(f"{h.D}/huisje_binnen/kamers.json", uit)
    # the game test room: a floor big enough for a huisje and (beside it) its room
    t = h.Structure((32, 10, 24))
    for x in range(32):
        for z in range(24):
            t.set(x, 0, z, "minecraft:grass_block", {"snowy": "false"})
    t.save("huisje_binnen_test")


# =====================================================================================================================
# the dimension
# =====================================================================================================================
def dimensie(h):
    D = h.D
    h.w(f"{D}/dimension/huisje_binnen.json", {"type": "guhs:huisje_binnen", "generator": {
        "type": "minecraft:flat", "settings": {"biome": "guhs:huisje_binnen", "layers": [], "lakes": False, "features": False,
                                               "structure_overrides": []}}})
    # written in the 26.1 shape (like guhpixel): a fixed soft light, beds work at any time and never set the spawn point
    h.w(f"{D}/dimension_type/huisje_binnen.json", {
        "attributes": {
            "minecraft:gameplay/bed_rule": {"can_set_spawn": "never", "can_sleep": "always"},
            "minecraft:gameplay/can_start_raid": False,
            "minecraft:gameplay/respawn_anchor_works": False,
            "minecraft:visual/ambient_light_color": "#3a2a32",
            "minecraft:visual/cloud_color": "#00ffffff",
            "minecraft:visual/fog_color": "#f6a8c8",
            "minecraft:visual/sky_color": "#f6a8c8",
        },
        "has_skylight": True, "has_ceiling": False, "has_ender_dragon_fight": False, "coordinate_scale": 1.0,
        "ambient_light": 0.25, "min_y": 0, "height": 256, "logical_height": 256,
        "infiniburn": "#minecraft:infiniburn_overworld", "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0,
        "has_fixed_time": True, "timelines": "#minecraft:universal"})
    h.w(f"{D}/worldgen/biome/huisje_binnen.json", {
        "has_precipitation": False, "temperature": 0.8, "downfall": 0.0,
        "effects": {"sky_color": 0xF6A8C8, "fog_color": 0xF6A8C8, "water_color": 0x7AD0FF, "water_fog_color": 0x5AA0E0},
        "spawners": {}, "spawn_costs": {}, "carvers": {"air": []}, "features": []})


# =====================================================================================================================
# the two blocks
# =====================================================================================================================
def _raam():
    """A little window with a painted view: a blue sky, a sun, green hills and a pink flower, in a rosewood frame."""
    img = Image.new("RGBA", (16, 16), (176, 92, 110, 255))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 13, 13], fill=(150, 212, 255, 255))
    d.rectangle([2, 2, 13, 5], fill=(176, 226, 255, 255))
    d.ellipse([9, 3, 12, 6], fill=(255, 236, 120, 255))
    d.rectangle([3, 4, 5, 4], fill=(255, 255, 255, 255))
    d.rectangle([4, 3, 5, 3], fill=(255, 255, 255, 255))
    d.ellipse([-2, 9, 9, 18], fill=(120, 196, 104, 255))
    d.ellipse([6, 10, 17, 20], fill=(98, 176, 96, 255))
    d.rectangle([2, 13, 13, 13], fill=(98, 176, 96, 255))
    for x, y, c in ((5, 11, (255, 150, 200)), (10, 12, (255, 255, 255)), (7, 12, (255, 236, 120))):
        img.putpixel((x, y), c + (255,))
    for x in range(16):                      # the frame again over what the ellipses painted, and the window cross
        for y in range(16):
            if x < 2 or x > 13 or y < 2 or y > 13:
                img.putpixel((x, y), (176, 92, 110, 255))
    d.line([(7, 2), (7, 13)], fill=(200, 116, 132, 255))
    d.line([(2, 7), (13, 7)], fill=(200, 116, 132, 255))
    d.rectangle([0, 0, 15, 15], outline=(150, 70, 90, 255))
    return img


def blokken(h):
    A = h.A
    h.save(_raam(), "block", "huisje_raam.png")
    h.w(f"{A}/models/block/huisje_raam.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "guhs:block/huisje_raam"}})
    h.w(f"{A}/blockstates/huisje_raam.json", {"variants": {"": {"model": "guhs:block/huisje_raam"}}})
    # the guhbedje (facing = where the pillow lies): a low rosewood frame with a headboard, a white mattress, a pink pillow
    hout, wit, roze = "#hout", "#wit", "#roze"
    els = [
        h.el([1, 0, 0], [15, 4, 16], hout),
        h.el([1, 4, 0], [15, 10, 1.5], hout),
        h.el([2, 4, 1.5], [14, 7, 15.5], wit),
        h.el([3.5, 7, 2], [12.5, 8.5, 6], roze),
    ]
    h.w(f"{A}/models/block/huisje_bedje.json", {"parent": "minecraft:block/block", "textures": {
        "particle": "minecraft:block/cherry_planks", "hout": "minecraft:block/cherry_planks", "wit": "minecraft:block/white_wool",
        "roze": "minecraft:block/pink_wool"}, "elements": els})
    h.w(f"{A}/blockstates/huisje_bedje.json", {"variants": h.facing_states("huisje_bedje")})


# =====================================================================================================================
# sounds, texts
# =====================================================================================================================
SOUNDS = {
    "huisje.bel": [{"name": "minecraft:block.note_block.bell", "type": "event", "volume": 0.8}],
}

TEXTS = {
    "block.guhs.huisje_bedje": "Guhbedje",
    "block.guhs.huisje_raam": "Huisjesraampje",
    "entity.guhs.huisje_slaper": "Slapende guh",
    "dimension.guhs.huisje_binnen": "In een Guhhuisje",
    "subtitles.guhs.huisje.bel": "Ding-dong, njeg",
    # the button in the huisje screen
    "gui.guhs.huisje.binnen.knop": "Naar binnen",
    "gui.guhs.huisje.binnen.knop.tooltip": "Loop door het snoetdeurtje het huisje in. Je bent wel een beetje groot, njeg!",
    # going in and out
    "gui.guhs.huisje.binnen.weiger.afstappen": "Eerst afstappen, njeg! Zo pas je niet door het deurtje.",
    "gui.guhs.huisje.binnen.weiger.spel": "Je bent nog met een spelletje bezig. Maak dat eerst af, njeg!",
    "gui.guhs.huisje.binnen.weiger.wakker": "Eerst wakker worden, njeg!",
    "gui.guhs.huisje.binnen.weiger.storing": "Het deurtje klemt even. Probeer het zo nog eens, njeg.",
    "gui.guhs.huisje.binnen.welkom": "Welkom in %s. Voeten vegen, njeg!",
    "gui.guhs.huisje.binnen.welkom_gast": "Welkom in %s, het huisje van %s. Voeten vegen, njeg!",
    "gui.guhs.huisje.binnen.buiten": "Je staat weer buiten. Dag huisje, njeg!",
    "gui.guhs.huisje.binnen.huisje_weg": "Het huisje staat er niet meer! Je staat weer buiten, njeg.",
    "gui.guhs.huisje.binnen.op_de_mat": "Hup, terug op de deurmat, njeg!",
    "gui.guhs.huisje.binnen.niet_hier": "Hier kom je alleen door het snoetdeurtje van een Guhhuisje, njeg.",
    # looking around
    "gui.guhs.huisje.binnen.van_wie": "Dit is het huisje van %s, njeg. Kijken mag, aankomen niet!",
    "gui.guhs.huisje.binnen.instoppen": "Je stopt %s lekker in. Welterusten, njeg! ♥",
    "gui.guhs.huisje.binnen.aaien": "Je aait %s heel zachtjes. Het snurkt gewoon door, njeg.",
    "gui.guhs.huisje.binnen.leeg_bed": "Dit bedje is nog vrij. Wie komt hier wonen, njeg?",
    "gui.guhs.huisje.binnen.maatje": "%s ligt lekker onder het dekentje. Ssst, njeg!",
    "gui.guhs.huisje.binnen.maatje_weg": "%s is even op pad, njeg.",
    "gui.guhs.huisje.binnen.bordje.vrij": "vrij bedje",
    "gui.guhs.huisje.binnen.briefje.buiten": "Ben buiten aan het spelen, njeg!",
    "gui.guhs.huisje.binnen.briefje.klus": "Ben een klusje doen: %s",
    "gui.guhs.huisje.binnen.briefje.speelt": "Ben met het speelgoed aan het spelen, njeg!",
    "gui.guhs.huisje.binnen.briefje.vakantie": "Ben op vakantie: %s. Kaartje volgt, njeg!",
    "gui.guhs.huisje.binnen.briefje.mee": "Ben mee met het baasje, njeg!",
    "gui.guhs.huisje.binnen.briefje.kantoor": "Ben op het Guhkantoor. Vadsig aan het werk, njeg.",
    "gui.guhs.huisje.binnen.briefje.guhkamer": "Logeer even in de Guhkamer, njeg!",
    "gui.guhs.huisje.binnen.briefje.parkour": "Ben op de Guh-parkour. VAHOEG!",
    "gui.guhs.huisje.binnen.briefje.guhpixel": "Ben even naar Guhpixel, njeg!",
    "gui.guhs.huisje.binnen.briefje.zit": "Zit ergens braaf te wachten, njeg.",
    "gui.guhs.huisje.binnen.briefje.wolkjes": "Ben in de wolkjes... njeg",
    "gui.guhs.huisje.binnen.briefje.weg": "Ben even weg. Zo terug, njeg!",
    "gui.guhs.huisje.binnen.prikbord.kop": "Prikbord van %s: wie doet welk klusje?",
    "gui.guhs.huisje.binnen.prikbord.regel": "%s: %s",
    "gui.guhs.huisje.binnen.prikbord.geen": "geen klusjes",
    "gui.guhs.huisje.binnen.prikbord.baby": "nog te klein voor klusjes",
    "gui.guhs.huisje.binnen.prikbord.leeg": "Er woont nog niemand. Het prikbord is nog leeg, njeg.",
    # the logeerbedje
    "gui.guhs.huisje.binnen.slaap.dag": "De guhs slapen pas als het donker is. Jij ook, njeg!",
    "gui.guhs.huisje.binnen.slaap.welterusten": "Welterusten tussen de guhs, njeg!",
    "gui.guhs.huisje.binnen.slaap.ochtend": "Goeiemorgen! Lekker geslapen tussen de guhs, njeg?",
    "gui.guhs.huisje.binnen.slaap.bezet": "In het logeerbedje ligt al iemand, njeg.",
}


def sounds(h):
    def patch(d):
        for event, entries in SOUNDS.items():
            d[event] = {"sounds": entries, "subtitle": f"subtitles.guhs.{event}"}
    h.patch_json(f"{h.A}/sounds.json", patch)


def build(h):
    dimensie(h)
    blokken(h)
    kamers(h)
    sounds(h)
    h.w(f"{h.D}/advancement/quest/huisje_binnen.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
    for key, nl in TEXTS.items():
        h.lang(key, nl, nl)
    selfcheck(h)


def selfcheck(h):
    missing = []
    for p in ([f"{h.D}/structure/huisje_binnen/{m}.nbt" for m in MATEN]
              + [f"{h.D}/huisje_binnen/kamers.json", f"{h.D}/dimension/huisje_binnen.json", f"{h.D}/dimension_type/huisje_binnen.json",
                 f"{h.D}/worldgen/biome/huisje_binnen.json", f"{h.A}/blockstates/huisje_bedje.json", f"{h.A}/models/block/huisje_bedje.json",
                 f"{h.A}/blockstates/huisje_raam.json", f"{h.A}/models/block/huisje_raam.json",
                 os.path.join(h.TEX, "block", "huisje_raam.png")]):
        if not os.path.exists(p):
            missing.append(p)
    for maat in MATEN:   # every room fits its cell and nothing stands on something else
        k = kamer(maat)
        if max(k["maat"][0], k["maat"][2]) > 16 or k["maat"][1] > 6:
            missing.append(f"room {maat} is larger than 16 x 6 x 16: {k['maat']}")
        plekken = [k["mat"], k["prikbord"], k["logeerbed"]["hoofd"], k["logeerbed"]["voet"], k["bak"]] + list(k["deur"])
        for b in k["bedden"]:
            plekken += [b["bed"], b["bord"], b["kastje"], b["haak"]]
        if len(set(plekken)) != len(plekken):
            missing.append(f"room {maat}: two things on one spot")
    for key in TEXTS:
        if key not in h.NL:
            missing.append(f"lang {key}")
    if missing:
        raise SystemExit(f"huisje_binnen assets missing: {missing}")
