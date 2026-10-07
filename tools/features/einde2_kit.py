"""
einde2 (the Guheinde expansion): the helper library of every einde2 module (not a feature itself: not in features.FEATURES).

    import features.einde2_kit as kit

  - the numbers every slice shares: the height bands of the Guheinde, the six biomes of the outer islands, their block
    palettes (PALET) and the placement table of the twelve big places (STRUCTUREN: salt, sector, ring, voorrang, biomes);
  - small writers: pagina (a Guhdex page), verborgen / zichtbaar (advancements), spawn_ei, stub_blok / stub_item /
    stub_structuur (the placeholders of the skeleton);
  - Bouw: guheinde_bouw.Bouw for a build that floats in the high band, with the markers ("plekken": where a character, a
    prop, a zone, a route, a ring or a creature goes), the overlay templates, save_tiles() and a self-check;
  - leegte_structuur / eiland_structuur: the ONLY places that write the worldgen JSON of an einde2 structure (the
    structure, its sets, its biome tag, its voorrang, its sidecar data/guhs/einde2/plekken/<name>.json and its name).

Java side: feature/einde2/Einde2Ids (the same numbers), LeegteBouwStructure (guhs:leegte_bouw), Plekken (reads the sidecars).
"""
import hashlib
import json
import math
import os
import re
from collections import deque

from features import guheinde_bouw as gb

# hook fase B (after the merge onto the sister update): every guaranteed einde2 set gets "alleen_nieuw": true, so its one
# copy only lands in terrain nobody has been to
EINDE2_ALLEEN_NIEUW = False
HOOG_MIN, HOOG_MAX, NATUUR_MAX, EILANDBOUW_MAX, TILE = 134, 253, 119, 127, 32
STRAAL_MIDDEN = 960          # within this many blocks of 0,0 the biome stays guhs:guheinde
MIN_AFSTAND = 1100           # no einde2 structure starts nearer to 0,0 than this
SECTOREN = 16                # the ring of every guaranteed einde2 set is cut into this many slices
MAX_VOETAFDRUK, MAX_HOOGTE, MAX_ANKER, MAX_VOORBIJ_ANKER = 256, 120, 120, 136

BUITEN = ["gatenkaasheuvels", "korstwoestijn", "kruimelwolken", "schimmelkaasvlakte", "kristalkaasklippen", "knabbelkoorbos"]
GEWOON, ZELDZAAM = BUITEN[:2], BUITEN[2:]
ELEMENT = {"lucht": "kruimelwolken", "water": "schimmelkaasvlakte", "aarde": "kristalkaasklippen", "vuur": "knabbelkoorbos"}
ALLE_ZES = "#guhs:einde2/buiteneilanden"

# biome -> the blocks of that biome a builder uses (the biomes slice makes them; ids of the build contract 3.3)
PALET = {
    "gatenkaasheuvels": {"grond": "guhs:kaaskorst", "steen": "guhs:kruimelrots", "stenen": "guhs:kruimelrots_stenen",
                         "trap": "guhs:kruimelrots_stenen_trap", "plaat": "guhs:kruimelrots_stenen_plaat", "muur": "guhs:kruimelrots_stenen_muur",
                         "plant": "guhs:korstgras", "licht": "guhs:lichtstaafje"},
    "korstwoestijn": {"grond": "guhs:gebarsten_korst", "steen": "guhs:kaaskorst", "stenen": "guhs:kaaskorst_stenen",
                      "trap": "guhs:kaaskorst_stenen_trap", "plaat": "guhs:kaaskorst_stenen_plaat", "muur": "guhs:kaaskorst_stenen_muur",
                      "zand": "guhs:kruimelzand", "zandlaag": "guhs:kruimelzandlaag", "plant": "guhs:dorre_korststruik", "licht": "guhs:lichtstaafje"},
    "kruimelwolken": {"grond": "guhs:kaaskorst", "steen": "guhs:kruimelrots", "stenen": "guhs:kruimelrots_stenen",
                      "trap": "guhs:kruimelrots_stenen_trap", "plaat": "guhs:kruimelrots_stenen_plaat", "muur": "guhs:kruimelrots_stenen_muur",
                      "wolk": "guhs:kruimelwolk", "dichte_wolk": "guhs:dichte_kruimelwolk", "licht": "guhs:lichtstaafje"},
    "schimmelkaasvlakte": {"grond": "guhs:schimmelkaas", "steen": "guhs:schimmelkaas", "stenen": "guhs:schimmelkaas_stenen",
                           "trap": "guhs:schimmelkaas_stenen_trap", "plaat": "guhs:schimmelkaas_stenen_plaat", "muur": "guhs:schimmelkaas_stenen_muur",
                           "plant": "guhs:schimmelpaddenstoel", "hoed": "guhs:schimmelhoed", "steel": "guhs:schimmelsteel",
                           "licht": "guhs:schimmelpaddenstoel"},
    "kristalkaasklippen": {"grond": "guhs:klipkaas", "steen": "guhs:klipkaas", "stenen": "guhs:klipkaas_stenen",
                           "trap": "guhs:klipkaas_stenen_trap", "plaat": "guhs:klipkaas_stenen_plaat", "muur": "guhs:klipkaas_stenen_muur",
                           "erts": "guhs:kristalkaas_erts", "kristal": "guhs:kristalkaas_kristal", "blok": "guhs:kristalkaas_blok",
                           "licht": "guhs:kristalkaas_kristal"},
    "knabbelkoorbos": {"grond": "guhs:koorkorst", "steen": "guhs:knabbelkoorblok", "stenen": "guhs:knabbelkoorblok",
                       "trap": "guhs:knabbelkoorblok_trap", "plaat": "guhs:knabbelkoorblok_plaat", "pilaar": "guhs:knabbelkoorpilaar",
                       "plant": "guhs:knabbelkoorplant", "bloem": "guhs:knabbelkoorbloem", "licht": "guhs:lichtstaafje"},
}

VERHAAL = ["zuidelijke_luchttempel", "noordelijke_waterstam", "ba_sing_guh", "vuurnatie_hoofdstad"]   # (in chapter order)
ZIJ = ["omashguh", "westelijke_luchttempel", "vuurtempel_roku", "kyoshi_dorp", "bibliotheek_wan_shi_guh", "leeuwschildpad_eiland",
       "leegtehaven", "guhvleugel_ringenrace"]


def _plek(slice_, salt, sector, ring, voorrang, biomes=None, rand=0, spreiding=None, kompas=None):
    return {"slice": slice_, "salt": salt, "sector": sector, "ring": ring, "voorrang": voorrang, "biomes": biomes or list(BUITEN), "rand": rand,
            "spreiding": spreiding, "kompas": kompas}


# the placement of the twelve big places (build contract 6.4). ring = blocks from 0,0 of the one guaranteed copy, sector = its
# slice of that ring (of SECTOREN), spreiding = (spacing, separation) in chunks of the very rare random copies (None: only
# the guaranteed copy), rand = blocks kept free around the box, kompas = its Superkompas tab. Never change a salt.
STRUCTUREN = {
    "zuidelijke_luchttempel": _plek("bouw_lucht", 21400901, 0, (2000, 3200), 400, rand=48),
    "noordelijke_waterstam": _plek("bouw_water", 21401001, 4, (2000, 3200), 399, rand=48),
    "ba_sing_guh": _plek("bouw_aarde", 21401101, 8, (2000, 3200), 398, rand=48),
    "vuurnatie_hoofdstad": _plek("bouw_vuur", 21401201, 12, (2000, 3200), 397, rand=48),
    "omashguh": _plek("bouw_zij1", 21401301, 2, (1300, 2600), 300, ["kristalkaasklippen"], spreiding=(96, 40), kompas="einde"),
    "westelijke_luchttempel": _plek("bouw_zij1", 21401311, 6, (1300, 2600), 299, ["kruimelwolken"], spreiding=(96, 40), kompas="einde"),
    "vuurtempel_roku": _plek("bouw_zij1", 21401321, 10, (1300, 2600), 298, ["knabbelkoorbos"], spreiding=(96, 40), kompas="einde"),
    "kyoshi_dorp": _plek("bouw_zij2", 21401401, 14, (1300, 2600), 297, ["schimmelkaasvlakte"], spreiding=(96, 40), kompas="einde"),
    "bibliotheek_wan_shi_guh": _plek("bouw_zij2", 21401411, 1, (1300, 2600), 296, ["korstwoestijn"], spreiding=(96, 40), kompas="einde"),
    "leeuwschildpad_eiland": _plek("bouw_zij2", 21401421, 5, (1500, 2800), 295, kompas="einde"),
    "leegtehaven": _plek("bouw_zij2", 21401431, 9, (1200, 2000), 301, kompas="einde"),
    "guhvleugel_ringenrace": _plek("bouw_zij2", 21401441, 13, (1300, 2600), 294, ["kruimelwolken"], kompas="einde"),
}

# the markers every guhs:leegte_bouw has, and the ones the four story structures have on top (build contract 6.6)
STANDAARD = ["aankomst", "aankomst_speler", "uitzicht"]
VERHAAL_STANDAARD = ["kamp", "appa_kamp", "team_aang", "team_momo", "team_katara", "team_sokka", "team_toph", "team_zuka", ("rustpunt", 3)]
SOORTEN = ("npc", "prop", "zone", "pad", "ring", "wezen")

# light for the self-check: guheinde_bouw's table plus the lights of einde2 and the vanilla ones a builder reaches for
LICHT = dict(gb.LIGHT)
LICHT.update({"guhs:lichtstaafje": 14, "guhs:kristalkaas_kristal": 12, "guhs:kristalkaas_blok": 10, "guhs:schimmelpaddenstoel": 9,
              "guhs:kruimelkwal_pot": 12, "guhs:knabbelkoorbloem": 6, "guhs:ringenrace_ring": 12, "minecraft:glowstone": 15,
              "minecraft:sea_lantern": 15, "minecraft:lantern": 15, "minecraft:soul_lantern": 10, "minecraft:torch": 14,
              "minecraft:wall_torch": 14, "minecraft:soul_torch": 10, "minecraft:soul_wall_torch": 10, "minecraft:end_rod": 14,
              "minecraft:shroomlight": 15, "minecraft:jack_o_lantern": 15, "minecraft:campfire": 15, "minecraft:soul_campfire": 10,
              "minecraft:lava": 15, "minecraft:fire": 15, "minecraft:magma_block": 3, "minecraft:ochre_froglight": 15,
              "minecraft:verdant_froglight": 15, "minecraft:pearlescent_froglight": 15, "minecraft:candle": 3, "minecraft:beacon": 15,
              "minecraft:crying_obsidian": 10, "minecraft:amethyst_cluster": 5, "minecraft:glow_lichen": 7})
WATER = ("minecraft:water", "guhs:kaas_saus")
# small things you walk through (on top of guheinde_bouw's thin blocks) and things you climb
DUN = ("korstgras", "korststruik", "schimmelpaddenstoel", "kristalkaas_kristal", "lichtstaafje", "minecraft:rail", "_rail", "flower", "_tulip",
       "minecraft:short_grass", "minecraft:tall_grass", "minecraft:fern", "minecraft:snow", "minecraft:end_rod", "minecraft:candle", "_banner",
       "minecraft:lever", "minecraft:tripwire", "minecraft:redstone_wire", "minecraft:light", "minecraft:structure_void", "glow_lichen",
       "minecraft:ladder", "minecraft:scaffolding", "_fence_gate", "trapdoor", "amethyst_cluster", "minecraft:fire", "sapling", "minecraft:dandelion",
       "minecraft:poppy")
KLIM = ("minecraft:ladder", "minecraft:scaffolding", "minecraft:vine")


def rng(naam):
    """A numpy generator seeded from a NAME (never from an index: the position of an einde2 module in features.FEATURES
    shifts at the merge onto the sister update, and with it the rng the variants() / clothes() hooks are handed)."""
    import numpy as np
    return np.random.default_rng(int.from_bytes(hashlib.sha256(f"einde2/{naam}".encode()).digest()[:8], "big"))


def seed(naam):
    """The same idea as a plain int (for random.Random and the noise textures)."""
    return int.from_bytes(hashlib.sha256(f"einde2/{naam}".encode()).digest()[:4], "big")


def lang(h, key, text):
    h.lang(key, text, text)


# =====================================================================================================================
# small writers
# =====================================================================================================================
def pagina(h, id, naam, zeldzaamheid, info, variant=False):
    """A Guhdex page: its name, rarity line and info, and the hidden advancement quest/seen_<id> (a variant: its name is
    entity.guhs.guh.<id> and quest/tamed_<id> is granted when someone tames one)."""
    lang(h, f"entity.guhs.guh.{id}" if variant else f"entity.guhs.{id}", naam)
    lang(h, f"gui.guhs.guhdex.rarity.{id}", "Zeldzaamheid: " + zeldzaamheid)
    lang(h, f"gui.guhs.guhdex.info.{id}", info)
    verborgen(h, f"seen_{id}")
    if variant:
        h.w(f"{h.D}/advancement/quest/tamed_{id}.json", {"criteria": {"done": {"trigger": "minecraft:tame_animal", "conditions": {
            "entity": [{"condition": "minecraft:entity_properties", "entity": "this",
                        "predicate": {"type": "guhs:guh", "nbt": "{Variant:\"%s\"}" % id}}]}}}})


def verborgen(h, name):
    """A hidden advancement guhs:quest/<name>, granted from code (GuhAdvancements.grant)."""
    h.w(f"{h.D}/advancement/quest/{name}.json", {"criteria": {"done": {"trigger": "minecraft:impossible"}}})


def zichtbaar(h, name, parent, icon, frame, titel, tekst, criteria=None):
    """A visible advancement guhs:guheinde/<name> in the Guheinde tab (criteria None: granted from code, GidsFeature.grant)."""
    h.w(f"{h.D}/advancement/guheinde/{name}.json", {
        "parent": f"guhs:guheinde/{parent}",
        "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.guhs.guheinde.{name}.title"},
                    "description": {"translate": f"advancements.guhs.guheinde.{name}.description"},
                    "frame": frame, "show_toast": True, "announce_to_chat": frame != "task", "hidden": False},
        "criteria": criteria or {"done": {"trigger": "minecraft:impossible"}}})
    lang(h, f"advancements.guhs.guheinde.{name}.title", titel)
    lang(h, f"advancements.guhs.guheinde.{name}.description", tekst)


def spawn_ei(h, id, naam, basis_rgb, vlek_rgb):
    """The spawn egg item <id>_spawn_egg of entity <id>: model, baked texture (mc26 has no egg template any more) and name.
    The colours are 0xRRGGBB."""
    import mc26
    from PIL import Image
    egg = Image.alpha_composite(mc26.tint(mc26.vanilla_121("item/spawn_egg"), basis_rgb), mc26.tint(mc26.vanilla_121("item/spawn_egg_overlay"), vlek_rgb))
    h.save(egg, "item", f"{id}_spawn_egg.png")
    h.item_model(f"{id}_spawn_egg")
    lang(h, f"item.guhs.{id}_spawn_egg", naam)


def stub_blok(h, id, naam, rgb):
    """A placeholder block of the skeleton: a flat-colour texture, a cube model, drops itself, a Dutch name."""
    h.save(h.noise_tex(tuple(rgb), 7, seed(id)), "block", f"{id}.png")
    h.simple_block(id)
    h.self_drop(id)
    lang(h, f"block.guhs.{id}", naam)


def stub_item(h, id, naam, rgb):
    """A placeholder item of the skeleton: a flat-colour blob, a Dutch name."""
    from PIL import Image
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    tex = h.noise_tex(tuple(rgb), 10, seed(id))
    for x in range(16):
        for y in range(16):
            if math.hypot(x - 7.5, y - 7.5) < 5.6:
                img.putpixel((x, y), tex.getpixel((x, y)))
    h.save(img, "item", f"{id}.png")
    h.item_model(id)
    lang(h, f"item.guhs.{id}", naam)


def stub_structuur(h, id, titel):
    """The placeholder of a big place: a little lit platform as a real guhs:leegte_bouw with the set, salt, sector and ring
    of STRUCTUREN, so the id exists (the Superkompas lists it) and the placement can be tried. The owner slice replaces it."""
    b = Bouw(h, id, (5, 4, 5), STRUCTUREN[id]["salt"])
    b.fill(0, 0, 0, 4, 0, 4, "guhs:kaaskorst_stenen")
    b.set(2, 0, 2, "guhs:guh_kristal_lamp")
    b.zone("aankomst", 0, 1, 0, 4, 3, 4, wie="hier komt de echte plek nog")
    b.prop("aankomst_speler", 2, 1, 2)
    b.save_tiles()
    leegte_structuur(h, b, titel=titel, tooltip="Hier wordt nog aan gebouwd, njeg", y=HOOG_MIN + 26)
    return b


# =====================================================================================================================
# a build with markers
# =====================================================================================================================
class Loper(gb.Walker):
    """guheinde_bouw's walker for an einde2 build: also wades through water, walks through the small plants and lights of
    the outer islands and climbs ladders."""

    def passable(self, c):
        n = self.name(c)
        if n is not None and n != "natural" and (n in WATER or self.b.is_dun(n)):
            return True
        return super().passable(c)

    def fluid(self, c):
        return self.name(c) in WATER

    def klim(self, c):
        return self.name(c) in KLIM

    def standable(self, c):
        x, y, z = c
        return self.inside(c) and self.passable(c) and self.passable((x, y + 1, z)) and \
            (self.support((x, y - 1, z)) or self.fluid(c) or self.klim(c))

    def moves(self, c):
        out = super().moves(c)
        x, y, z = c
        if self.klim(c) or self.klim((x, y + 1, z)):
            up = (x, y + 1, z)
            if self.inside(up) and self.passable(up) and self.passable((x, y + 2, z)):
                out.append(up)
        if self.klim((x, y - 1, z)):
            out.append((x, y - 1, z))
        return out


class Bouw(gb.Bouw):
    """A build of einde2 in the making. Coordinates are TEMPLATE coordinates of the whole build; cells nobody sets stay what
    the world has (air, in the high band). prefix = the sign text prefix of the slice ("sign.guhs.einde2_<slice>")."""

    def __init__(self, h, name, size, seed, prefix=None):
        if not hasattr(h, "ms"):
            h.ms = __import__("make_structures")
        super().__init__(h, name, size, lambda c: False, seed)
        self.prefix = prefix
        self.plekken = {}        # id -> the sidecar entry
        self.lagen = {}          # naam -> {"template", "pos", "grootte"}
        self.los = set()         # ids of spots that are shut in or apart on purpose
        self.dun = set()         # extra block names the walker passes through (a slice's own small decoration)
        self.tiles = None        # (tiles_x, tiles_z, [(i, j)]) once saved
        self.fouten = []         # marker mistakes found while building

    def is_dun(self, n):
        return n in self.dun or gb.is_thin(n) or any(t in n for t in DUN)

    def solid_at(self, c):
        b = self.s.blocks.get(c)
        return b is not None and super().solid_at(c) and b[0] not in WATER and not self.is_dun(b[0])

    # --- text -----------------------------------------------------------------------------------------------------------
    def sign(self, x, y, z, facing, lines, wall=True, wood="birch", glow=False):
        """A waxed sign with up to four Dutch lines (each at most 16 characters); the text goes through sign_text, so every
        player reads it in their own language. Wall signs hang on the block behind them."""
        import sign_text
        assert self.prefix, f"{self.name}: Bouw(..., prefix='sign.guhs.einde2_<slice>') before sign()"
        for line in lines:
            if isinstance(line, str) and len(line) > 16:
                self.fouten.append(f"bordje {(x, y, z)}: '{line}' is langer dan 16 tekens")
        B = self.h.ms.Byte
        nbt = {"id": "minecraft:sign", "is_waxed": B(1),
               "front_text": {"messages": sign_text.messages(self.prefix, lines), "color": "black", "has_glowing_text": B(1 if glow else 0)},
               "back_text": {"messages": sign_text.messages(self.prefix, ["", "", "", ""]), "color": "black", "has_glowing_text": B(0)}}
        if wall:
            self.set(x, y, z, f"{wood}_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
        else:
            rot = {"south": "0", "west": "4", "north": "8", "east": "12"}[facing]
            self.set(x, y, z, f"{wood}_sign", {"rotation": rot, "waterlogged": "false"}, nbt)

    # --- markers ----------------------------------------------------------------------------------------------------------
    def _merk(self, id, entry, los=False):
        if not re.fullmatch(r"[a-z][a-z0-9_]*", id):
            self.fouten.append(f"plek '{id}': alleen kleine letters, cijfers en _")
        if id in self.plekken:
            self.fouten.append(f"plek '{id}' staat er twee keer")
        self.plekken[id] = {k: v for k, v in entry.items() if v not in (None, "")}
        if los:
            self.los.add(id)

    def plek(self, id, x, y, z, yaw=0.0, wie="", kind=None, los=False):
        """Where a character stands (soort npc): the block its feet are in, the way it looks (Minecraft yaw: 0 south, 90 west,
        180 north, -90 east), who it is and the GuhNpcEntity.Kind name reserved for it. NOTHING is placed: the spot stays free.
        los=True: shut in or apart on purpose (a cell, a ship before the sneak route): not checked for reachability."""
        self._merk(id, {"soort": "npc", "pos": [x, y, z], "yaw": float(yaw), "wie": wie, "kind": kind}, los)

    def prop(self, id, x, y, z, wie="", los=False):
        """A block position a later phase reads or changes (a door, a lever spot, a table, a pedestal, where a player stands)."""
        self._merk(id, {"soort": "prop", "pos": [x, y, z], "wie": wie}, los)

    def zone(self, id, x0, y0, z0, x1, y1, z1, wie=""):
        """A box (two corners, inclusive)."""
        self._merk(id, {"soort": "zone", "pos": [min(x0, x1), min(y0, y1), min(z0, z1)], "tot": [max(x0, x1), max(y0, y1), max(z0, z1)], "wie": wie})

    def pad(self, id, punten, wie=""):
        """An ordered list of points (a route, a track, a canal)."""
        self._merk(id, {"soort": "pad", "punten": [list(map(int, p)) for p in punten], "wie": wie})

    def ring(self, id, x, y, z, as_="x", straal=4):
        """A ring to fly through: its centre, the axis it faces along and its radius."""
        self._merk(id, {"soort": "ring", "pos": [x, y, z], "as": as_, "straal": straal})

    def wezen(self, id, x, y, z, entity, yaw=0.0, honk=8, **nbt):
        """A creature of phase A: placed for real (PersistenceRequired, and Honk: it stays within that many blocks of this
        spot) and recorded too. Extra NBT as keyword arguments (AttachFace=..., Handelaar=...)."""
        B, fl = self.h.ms.Byte, self.h.ms.floats
        data = {"id": entity, "PersistenceRequired": B(1), "Honk": int(honk), "Rotation": fl(float(yaw), 0.0)}
        data.update(nbt)
        self.ent(x + 0.5, y, z + 0.5, data, entity)
        self._merk(id, {"soort": "wezen", "pos": [x, y, z], "yaw": float(yaw), "entity": entity})

    def laag(self, naam, structure, x, y, z):
        """An overlay template <name>/laag_<naam> (a Structure of its own, blocks only) that a later phase puts on the build
        at this spot and takes away again (Plekken.plaatsLaag / ruimLaag). Its origin and size are recorded."""
        if structure.entities:
            self.fouten.append(f"laag {naam}: een laag heeft alleen blokken (geen entities: die blijven staan als de laag weggaat)")
        if naam in self.lagen:
            self.fouten.append(f"laag '{naam}' staat er twee keer")
        sx, sy, sz = structure.size
        if not (self.inside(x, y, z) and self.inside(x + sx - 1, y + sy - 1, z + sz - 1)):
            self.fouten.append(f"laag {naam} steekt buiten de bouw")
        structure.save(f"{self.name}/laag_{naam}")
        self.lagen[naam] = {"template": f"guhs:{self.name}/laag_{naam}", "pos": [x, y, z], "grootte": [sx, sy, sz]}

    # --- the self-check ---------------------------------------------------------------------------------------------------
    def licht(self):
        """Block light per cell, from the light sources of LICHT (the dimension has no skylight)."""
        blocks = self.s.blocks

        def opaque(c):
            bl = blocks.get(c)
            if bl is None:
                return False
            n = bl[0]
            return n != gb.AIR and n not in WATER and not self.is_dun(n) and not any(t in n for t in gb.TRANSPARENT_SUB) and n not in LICHT

        light, todo = {}, deque()
        for c, bl in blocks.items():
            if LICHT.get(bl[0]):
                light[c] = LICHT[bl[0]]
                todo.append(c)
        while todo:
            c = todo.popleft()
            l = light[c] - 1
            if l <= 0:
                continue
            x, y, z = c
            for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                n = (x + dx, y + dy, z + dz)
                if self.inside(*n) and light.get(n, 0) < l and not opaque(n):
                    light[n] = l
                    todo.append(n)
        return light

    def missend(self, verwacht):
        """The ids of `verwacht` that are not there. An entry is an id, or (name, n): at least n spots <name>_1, <name>_2...
        (also <name>_01...)."""
        out = []
        for v in verwacht:
            if isinstance(v, str):
                if v not in self.plekken:
                    out.append(v)
            else:
                name, n = v
                have = sum(1 for k in self.plekken if re.fullmatch(re.escape(name) + r"_\d+", k))
                if have < n:
                    out.append(f"{name}_* ({have} van {n})")
        return out

    def check(self, start, verwacht=(), min_licht=4, zwevend_ok=None, standaard=True):
        """The problems of this build (an empty list: fine):
          - blocks put outside the template, marker mistakes (double ids, a sign line that is too long, an overlay with entities);
          - blocks that hang loose: not connected to the island you stand on at `start` (zwevend_ok(c) -> True excuses a cell:
            a ring of the race, a cloud, a moored ship);
          - lanterns, signs, doors, carpets, chests that hang or stand on nothing;
          - every id of `verwacht` (and, with standaard, the standard markers; the story markers for a story structure);
          - every npc spot: two free blocks on a solid one, and you can walk there from `start`; every prop: you can stand on
            it or right next to it; unless it was recorded with los=True;
          - zones, pads and rings inside the build (only `uitzicht` lies outside: up to 40 blocks off the rim);
          - the aankomst zone at least 9 x 9;
          - a walkable floor darker than min_licht.
        start = where a player stands after arriving (the aankomst_speler spot)."""
        p = list(self.fouten)
        if self.oob:
            p.append(f"{len(self.oob)} blokken buiten de bouw, bv. {self.oob[:4]}")
        start = tuple(start)
        w = Loper(self)
        if not w.standable(start):
            return p + [f"start {start} is niet beloopbaar"]
        reach = w.bfs(start)
        onder = (start[0], start[1] - 1, start[2])
        los = gb.floating(self, lambda c: c == onder)
        if zwevend_ok:
            los = {c for c in los if not zwevend_ok(c)}
        los = {c for c in los if not self.is_dun(self.s.blocks[c][0]) and "sign" not in self.s.blocks[c][0]}
        if los:
            p.append(f"{len(los)} zwevende blokken, bv. {sorted(los)[:5]}")
        p += gb.attach_problems(self)
        nodig = list(verwacht)
        if standaard:
            nodig += STANDAARD + (VERHAAL_STANDAARD if self.name in VERHAAL else [])
        mis = self.missend(nodig)
        if mis:
            p.append(f"plekken ontbreken: {mis}")
        for id, e in self.plekken.items():
            punten = [e["pos"]] if "pos" in e else []
            punten += [e["tot"]] if "tot" in e else []
            punten += e.get("punten", [])
            if id == "uitzicht":
                if any(not (-40 <= q[0] < self.W + 40 and 0 <= q[1] < self.H and -40 <= q[2] < self.D + 40) for q in punten):
                    p.append("plek uitzicht ligt verder dan 40 blokken buiten de bouw")
                continue
            if any(not self.inside(*q) for q in punten):
                p.append(f"plek {id} ligt buiten de bouw: {punten[:2]}")
                continue
            if id in self.los:
                continue
            c = tuple(e.get("pos", (0, 0, 0)))
            if e["soort"] == "npc":
                if not w.standable(c):
                    p.append(f"plek {id} {c}: geen twee vrije blokken op een vaste vloer")
                elif c not in reach:
                    p.append(f"plek {id} {c}: daar kun je niet komen (los=True als dat zo hoort)")
            elif e["soort"] == "prop":
                if c not in reach and not gb.near(reach, c, dy=(-1, 0, 1)) and (c[0], c[1] + 1, c[2]) not in reach:
                    p.append(f"plek {id} {c}: daar kun je niet bij (los=True als dat zo hoort)")
        a = self.plekken.get("aankomst")
        if a and a.get("soort") == "zone" and standaard:
            if a["tot"][0] - a["pos"][0] + 1 < 9 or a["tot"][2] - a["pos"][2] + 1 < 9:
                p.append("de aankomst is kleiner dan 9 x 9")
        light = self.licht()
        donker = [c for c in reach if light.get(c, 0) < min_licht]
        if donker:
            p.append(f"{len(donker)} van {len(reach)} beloopbare plekken zijn te donker (< {min_licht}), bv. {sorted(donker)[:5]}")
        self.stats = {"blokken": len(self.s.blocks), "entities": len(self.s.entities), "beloopbaar": len(reach), "plekken": len(self.plekken),
                      "lagen": len(self.lagen)}
        return p

    # --- saving -----------------------------------------------------------------------------------------------------------
    def save_tiles(self):
        """Cuts the build into 32 x 32 tiles <name>/stuk_<i>_<j> (empty tiles are left out; tiles of an earlier, bigger build
        are removed). Returns (tiles_x, tiles_z)."""
        tiles, ents = {}, {}
        for (x, y, z), b in self.s.blocks.items():
            tiles.setdefault((x // TILE, z // TILE), {})[(x % TILE, y, z % TILE)] = b
        for (x, y, z, nbt) in self.s.entities:
            i, j = int(x) // TILE, int(z) // TILE
            ents.setdefault((i, j), []).append((x - i * TILE, y, z - j * TILE, nbt))
        nx, nz = (self.W + TILE - 1) // TILE, (self.D + TILE - 1) // TILE
        saved = []
        for i in range(nx):
            for j in range(nz):
                if (i, j) not in tiles and (i, j) not in ents:
                    continue
                t = self.h.Structure((min(TILE, self.W - i * TILE), self.H, min(TILE, self.D - j * TILE)))
                t.blocks = tiles.get((i, j), {})
                t.entities = ents.get((i, j), [])
                t.save(f"{self.name}/stuk_{i}_{j}")
                saved.append((i, j))
        folder = f"{self.h.D}/structure/{self.name}"
        keep = {f"stuk_{i}_{j}.nbt" for i, j in saved}
        for f in sorted(os.listdir(folder)) if os.path.isdir(folder) else []:
            if re.fullmatch(r"stuk_\d+_\d+\.nbt", f) and f not in keep:
                os.remove(os.path.join(folder, f))
        self.tiles = (nx, nz, saved)
        return nx, nz

    def blad(self, soort, anker=None):
        """The sidecar of this build (data/guhs/einde2/plekken/<name>.json)."""
        out = {"structuur": self.name, "soort": soort, "grootte": [self.W, self.H, self.D]}
        if anker is not None:
            out["anker"] = list(anker)
        out["plekken"] = self.plekken
        if self.lagen:
            out["lagen"] = self.lagen
        return out


# =====================================================================================================================
# worldgen: the only writers of an einde2 structure
# =====================================================================================================================
def standaard_anker(b):
    """The default anchor of a build: its middle, on the 16 k + 8 grid, at most MAX_ANKER and not too far from the far rim."""
    def as_(n):
        a = 8 + 16 * int(round((n / 2 - 8) / 16))
        a = max(8, min(MAX_ANKER, a))
        while n - a > MAX_VOORBIJ_ANKER and a + 16 <= MAX_ANKER:
            a += 16
        return a
    return (as_(b.W), 0, as_(b.D))


def gebruikte_salts(h, behalve=()):
    """salt -> structure set, of every set in the data pack (to keep a new salt unique)."""
    out = {}
    folder = f"{h.D}/worldgen/structure_set"
    for f in sorted(os.listdir(folder)):
        if f[:-5] in behalve:
            continue
        s = json.load(open(os.path.join(folder, f), encoding="utf-8"))
        if "salt" in s.get("placement", {}):
            out[s["placement"]["salt"]] = f[:-5]
    return out


def leegte_structuur(h, b, *, titel, tooltip, y, anker=None):
    """Everything of a guhs:leegte_bouw except its templates (call b.save_tiles() first, or it is done here): the structure,
    its guaranteed set (salt + 7), its very rare random set when STRUCTUREN says so, its biome tag, its voorrang, its sidecar
    and its name (+ Superkompas tooltip). y = the world y of template y 0 (in the high band). Asserts the limits of the type."""
    name, D = b.name, h.D
    info = STRUCTUREN[name]
    if b.tiles is None:
        b.save_tiles()
    nx, nz, saved = b.tiles
    anker = tuple(anker or standaard_anker(b))
    ax, ay, az = anker
    assert saved, f"{name}: no tiles"
    assert b.W <= MAX_VOETAFDRUK and b.D <= MAX_VOETAFDRUK, f"{name}: footprint {b.W} x {b.D} is more than {MAX_VOETAFDRUK}"
    assert b.H <= MAX_HOOGTE, f"{name}: {b.H} high is more than {MAX_HOOGTE}"
    assert ax % 16 == 8 and az % 16 == 8, f"{name}: anchor x and z must be 16 k + 8 (tiles line up with chunks): {anker}"
    assert 8 <= ax <= MAX_ANKER and 8 <= az <= MAX_ANKER, f"{name}: anchor at most {MAX_ANKER}: {anker}"
    assert b.W - ax <= MAX_VOORBIJ_ANKER and b.D - az <= MAX_VOORBIJ_ANKER, f"{name}: more than {MAX_VOORBIJ_ANKER} past the anchor (tiles beyond 8 chunks are never placed)"
    assert HOOG_MIN <= y and y + b.H - 1 <= HOOG_MAX, f"{name}: y {y}..{y + b.H - 1} is outside the high band {HOOG_MIN}..{HOOG_MAX}"
    for naam, laag in b.lagen.items():
        (x0, y0, z0), (sx, sy, sz) = laag["pos"], laag["grootte"]
        assert b.inside(x0, y0, z0) and b.inside(x0 + sx - 1, y0 + sy - 1, z0 + sz - 1), f"{name}: laag {naam} outside the build"
    lo, hi = info["ring"]
    behalve = (name, f"{name}_gegarandeerd")
    used = gebruikte_salts(h, behalve)
    assert info["salt"] not in used and info["salt"] + 7 not in used, f"{name}: salt in use by {used.get(info['salt']) or used.get(info['salt'] + 7)}"
    h.w(f"{D}/worldgen/structure/{name}.json", {
        "type": "guhs:leegte_bouw", "biomes": f"#guhs:has_structure/{name}", "step": "surface_structures", "terrain_adaptation": "none",
        "spawn_overrides": {"monster": {"bounding_box": "full", "spawns": []}, "creature": {"bounding_box": "full", "spawns": []}},
        "templates": f"guhs:{name}", "tiles_x": nx, "tiles_z": nz, "tile_size": TILE, "anchor": [ax, ay, az], "size": [b.W, b.H, b.D],
        "y": y, "min_distance": MIN_AFSTAND, "rand": info["rand"]})
    placement = {"type": "guhs:gegarandeerd", "salt": info["salt"] + 7, "min_afstand": lo, "max_afstand": hi, "sector": info["sector"],
                 "sectoren": SECTOREN}
    if EINDE2_ALLEEN_NIEUW:
        placement["alleen_nieuw"] = True
    h.w(f"{D}/worldgen/structure_set/{name}_gegarandeerd.json", {"structures": [{"structure": f"guhs:{name}", "weight": 1}], "placement": placement})
    spread = f"{D}/worldgen/structure_set/{name}.json"
    if info["spreiding"]:
        spacing, separation = info["spreiding"]
        h.w(spread, {"structures": [{"structure": f"guhs:{name}", "weight": 1}],
                     "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": separation, "salt": info["salt"]}})
    elif os.path.exists(spread):
        os.remove(spread)
    alle = sorted(info["biomes"]) == sorted(BUITEN)
    h.w(f"{D}/tags/worldgen/biome/has_structure/{name}.json", {"values": [ALLE_ZES] if alle else [f"guhs:{x}" for x in info["biomes"]]})
    h.VOORRANG[name] = info["voorrang"]
    h.w(f"{D}/einde2/plekken/{name}.json", b.blad("leegte_bouw", anker))
    lang(h, f"structure.guhs.{name}", titel)
    lang(h, f"structure.guhs.{name}.tooltip", tooltip)


def leegte_reach(s, jigsaw_reach=None):
    """make_v2.RUIMTE_HOOKS["guhs:leegte_bouw"]: the reach of the build from the middle of its start chunk, plus its rand."""
    (ax, _, az), (sx, _, sz) = s["anchor"], s["size"]
    return max(ax, sx - 1 - ax, az, sz - 1 - az) + 1 + s.get("rand", 0)


def eiland_structuur(h, name, *, start_pool, start_jigsaw, biomes, spacing, separation, salt, min_surface_y=52, check_radius=8,
                     size=1, reach=32, voorrang=None, spawn_overrides=None, plekken=None):
    """A guhs:guheinde_eiland structure on an outer island (the decoration buildings): a jigsaw start that only lands far
    from the main island and where the island is really there. Writes the structure, its random_spread set and its biome
    tag; asserts the salt is unused. plekken=[(b, "<template>")]: also a jigsaw sidecar (each marker local to its piece)."""
    D = h.D
    assert reach + 12 <= 128, f"{name}: max_distance_from_center {reach} + 12 is more than 128"
    used = gebruikte_salts(h, (name,))
    assert salt not in used, f"{name}: salt {salt} in use by {used[salt]}"
    tag = f"#guhs:has_structure/{name}"
    h.w(f"{D}/worldgen/structure/{name}.json", {
        "type": "guhs:guheinde_eiland", "biomes": tag, "step": "surface_structures", "spawn_overrides": spawn_overrides or {},
        "terrain_adaptation": "none", "min_distance": MIN_AFSTAND, "min_surface_y": min_surface_y, "check_radius": check_radius,
        "jigsaw": {"type": "minecraft:jigsaw", "biomes": tag, "step": "surface_structures", "spawn_overrides": {}, "terrain_adaptation": "none",
                   "start_pool": start_pool, "size": size, "start_height": {"absolute": 0}, "project_start_to_heightmap": "WORLD_SURFACE_WG",
                   "start_jigsaw_name": start_jigsaw, "max_distance_from_center": reach, "use_expansion_hack": False}})
    h.w(f"{D}/worldgen/structure_set/{name}.json", {
        "structures": [{"structure": f"guhs:{name}", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": separation, "salt": salt}})
    h.w(f"{D}/tags/worldgen/biome/has_structure/{name}.json", {"values": [x if ":" in x else f"guhs:{x}" for x in biomes]})
    if voorrang is not None:
        h.VOORRANG[name] = voorrang
    if plekken:
        jigsaw_blad(h, name, plekken)


def jigsaw_blad(h, name, plekken):
    """The sidecar of a jigsaw structure: plekken=[(b, "<template>")], every marker of b local to the piece made from that
    template (the same piece may be in a copy more than once: Plekken.alle gives every one)."""
    alle = {}
    for b, stuk in plekken:
        for id, e in b.plekken.items():
            assert id not in alle, f"{name}: plek {id} is in two pieces"
            alle[id] = dict(e, stuk=stuk)
    h.w(f"{h.D}/einde2/plekken/{name}.json", {"structuur": name, "soort": "jigsaw", "plekken": alle})
