"""
1.7.0 "Onder de Wol": the shared framework for the buildings under the ground (design: MineScapeStory/ontwerp-guhs170-mijnen.md
chapter 2.4; Java: feature/onderwol/bouw, structure type guhs:grot_bouw). Every underground structure of this update uses it.

ONE CALL PER STRUCTURE (in your module's build(h)):

    from features import onderwol_bouw as ob

    b = ob.Bouw((30, 12, 24), "mijn_kamertje", prefix="sign.guhs.onderwol_bouw.kamertje")   # a template (make_structures.Structure)
    ... b.set / b.fill / b.sign / b.npc / b.guh / b.kist ...
    b.anker(15, 2, 12)               # the start jigsaw IN the floor layer (you stand at y 3): lands on the chosen floor
    b.ingang(4, 2, 4)                # optional: where the shaft of an entrance hut comes down (a free 3 x 3 column above it)
    b.save("onderwol/kamertje/start")
    ob.grot_bouw(h, "kamertje", templates=[(b, 1)], titel="Mijnkamertje", tooltip="...", salt=17012001,
                 band=(30, 44), spacing=24, separation=8, zaal=dict(rand=4, boven=3), ingang=dict(kans=0.5))

  grot_bouw(h, naam, *, templates, titel, tooltip, salt, band, ...)
      templates  [(Bouw, weight)]: the start pool guhs:onderwol/<naam>/start (each with the anchor guhs:<naam>_midden)
      band       (min_y, max_y): the floor height (the y you stand on). Layers on average: wool above ~64, viltsteen
                 ~64-41, knuffelsteen ~41-20, guhkristalsteen below 20 (they follow the land a little). Never at or below
                 y 11 (the saus sea).
      ruimte     "beide" (default: on the floor of a cave that is already there when the middle of the chunk has one in the
                 band, else at a random height in the band), "grot" (only on an existing cave floor), "zaal" (always random)
      zaal       None, or dict(rand=4, boven=3, ruw=1.0, alle=False): carve a natural hall around the start piece (alle: around
                 every piece), rand blocks wider than it, boven blocks above it. Leave it out when the template carves its own
                 room (its air is air in the world).
      ingang     None, or dict(kans=1.0, verplicht=False, pool=INGANG_POOL, pogingen=...): the entrance hut on the surface
                 above the template's b.ingang(...) marker, with a shaft (ladder + wolkenlift up + down). Only on dry, flat
                 enough ground; verplicht: no hut, no building; pogingen (default 9 with kans 1.0, else 1): how many spots
                 (the chunk's middle, then 8 blocks to the sides and corners) the build tries until its hut fits.
      pools      {pool_name: [(Bouw, weight), ...]}: extra pools for jigsaw children (guhs:onderwol/<naam>/<pool_name>,
                 fallback guhs:onderwol/<naam>/<pool_name>_eind when that pool is given too, else minecraft:empty)
      size, max_distance   the jigsaw depth and reach (size 1 = only the start piece)
      spacing, separation  the random spread (chunks); gegarandeerd: dict(min=, max=, sector=, sectoren=): one copy per world in
                 that ring around 0,0, ALWAYS alleen_nieuw (in an existing world only in terrain that is not generated yet)
      voorrang   who goes first when two buildings would touch (default: its reach)
      dekking    blocks of rock kept above every piece (default 6)
      kompas     the Superkompas tab ("ondergrond"); the Java side adds the entry: OnderWolBouw.STRUCTUREN (other slices:
                 SuperkompasItem.voegToe("ondergrond", "<naam>") in their Feature.register)

THE TEMPLATE RULES (Bouw):
  - its AIR is carved out of the world; what is not in the template (nothing stored) keeps the world's own blocks;
  - SCHIL (bedrock) = the rock right around the rooms: the world's block stays, but where the world is open (a cave, a
    lake) it becomes the layer stone of that spot (processor guhs:onderwol_grot_schil). Use it for walls/floor/ceiling
    around carved rooms, so they never open into another cave or a lake by accident;
  - signs: b.sign(x, y, z, facing, ["max 16", "chars", "per", "line"]) (translate keys through sign_text);
  - talking guhs: b.npc(x, y, z, plek, yaw) for a plek of OnderWolBouw.PRAAT (its lines quest.guhs.onderwol_bouw.<plek>.<i>);
    the Mijnguh: b.mijnguh(x, y, z, yaw);
  - guhs: b.guh(x, y, z, yaw, slaapt=False, schaal=1.0) (slaapt: curled up asleep until petted, feature/onderwol/wezens/SlaapGuhs);
  - chests and barrels with loot: b.kist(x, y, z, facing, "guhs:chests/onderwol/<naam>"), b.ton(x, y, z, loot);
  - jigsaw connectors for children: b.verbinding(x, y, z, facing, naam, doel, pool) (horizontal, facing = out of the piece).

  loot(h, naam, pools) writes data/guhs/loot_table/chests/onderwol/<naam>.json from [(rolls, [(item, weight, min, max)])].

Standalone (no make_resources): the slice modules have a _direct() like onderwol.py. The entrance hut pool (INGANG_POOL) is
made by onderwol_structuren1.py (the Mijningang-hutje), so a structure with an ingang needs that module's templates.
"""
import json
import os

import make_structures as ms
import sign_text

TYPE = "guhs:grot_bouw"
PROCESSORS = "guhs:onderwol_grot_schil"
SCHIL = "minecraft:bedrock"
INGANG = "guhs:grot_ingang"
INGANG_BOVEN = "guhs:grot_ingang_boven"
INGANG_POOL = "guhs:onderwol/ingang_hutje/start"
STEP = "fluid_springs"   # (after the ores and the saus sea, like every guhs building: make_v2.bouwruimte)
# the biomes of the Guhmension the underground buildings may stand under (not the seas, the islands, the Wolkenweide, the
# Gatenkaasgrotten and the kristalmijn: those have no layers of their own or no rock to build in)
NIET = {"guhs:guh_sea", "guhs:diepe_guhzee", "guhs:guhwaii", "guhs:wolkenweide", "guhs:gatenkaasgrotten", "guhs:guh_kristalmijn"}
# the talking guhs (Java OnderWolBouw.PRAAT): plek -> kind
PRAAT_KIND = {"onderwol_dorp": "mijnwerkerguh", "onderwol_ingang": "mijnwerkerguh", "onderwol_mijn": "mijnwerkerguh",
              "onderwol_slaapzaal": "omaguh", "onderwol_paddenstoel": "hanami_bloesemguh", "onderwol_visser": "botenhuisje_visserguh",
              "onderwol_gids": "omaguh"}

STRUCTUREN = {}   # naam -> what grot_bouw wrote (for the checks)
# an entrance hut that is sure to come (kans 1.0): the build may try this many spots (Java GrotBouwStructure.SCHUIVEN, 8
# blocks to the sides and corners) until the ground above its shaft is fit for the hut; the reach grows by SCHUIF_MAX
POGINGEN = 9
SCHUIF_MAX = 12


def guhmension_biomes(h):
    d = json.load(open(f"{h.D}/dimension/guhmension.json", encoding="utf-8"))
    return sorted({b["biome"] for b in d["generator"]["biome_source"]["biomes"]} - NIET)


def _blok(s):
    """'guhs:x[a=b,c=d]' -> ('guhs:x', {'a': 'b', 'c': 'd'})"""
    if "[" in s:
        naam, rest = s.split("[", 1)
        props = dict(kv.split("=") for kv in rest.rstrip("]").split(",") if kv)
    else:
        naam, props = s, {}
    if ":" not in naam:
        naam = "guhs:" + naam
    return naam, props


ROT = {"south": "0", "west": "4", "north": "8", "east": "12"}


class Bouw(ms.Structure):
    """A template for guhs:grot_bouw (see the module text)."""

    def __init__(self, size, naam, prefix=None):
        super().__init__(tuple(size))
        self.naam = naam
        self.prefix = prefix or f"sign.guhs.onderwol_bouw.{naam}"
        self.ankers = {}        # jigsaw name -> (x, y, z)
        self.fouten = []
        self.nepdeuren = set()  # (x, y, z) of door lower halves that may stand against a wall (decoration: deuren_vrij)

    def zet(self, x, y, z, s, nbt=None):
        """set() with a block string 'name[k=v]' (no namespace = guhs)."""
        naam, props = _blok(s)
        self.set(x, y, z, naam, props, nbt)

    def schil(self, x, y, z):
        self.set(x, y, z, SCHIL)

    def leeg(self, x, y, z):
        """Nothing here: the world's own block stays (removes what was set)."""
        self.blocks.pop((int(x), int(y), int(z)), None)

    def _jigsaw(self, x, y, z, orientation, naam, doel="minecraft:empty", pool="minecraft:empty", final="minecraft:air", joint="rollable"):
        self.set(x, y, z, "minecraft:jigsaw", {"orientation": orientation},
                 {"id": "minecraft:jigsaw", "name": naam, "target": doel, "pool": pool, "final_state": final, "joint": joint,
                  "placement_priority": 0, "selection_priority": 0})
        self.ankers[naam] = (int(x), int(y), int(z))

    def anker(self, x, y, z, final=None):
        """The start jigsaw guhs:<naam>_midden IN the floor layer (the building's floor y is y + 1). final: the block it becomes
        (default: the block that was set there, else SCHIL)."""
        if final is None:
            b = self.blocks.get((x, y, z))
            final = _staat(b) if b and b[0] != "minecraft:jigsaw" else SCHIL
        self._jigsaw(x, y, z, "up_north", f"guhs:{self.naam}_midden", final=final)

    def ingang(self, x, y, z, final=None):
        """Where the shaft of the entrance hut comes down: in the floor layer, with a free 3 x 3 column above it (cleared
        here: four blocks of air above the floor)."""
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                for dy in range(1, 5):
                    if (x + dx, y + dy, z + dz) in self.blocks:
                        self.set(x + dx, y + dy, z + dz, "minecraft:air")
        if final is None:
            b = self.blocks.get((x, y, z))
            final = _staat(b) if b and b[0] != "minecraft:jigsaw" else SCHIL
        self._jigsaw(x, y, z, "up_north", INGANG, final=final)

    def ingang_boven(self, x, y, z, richting):
        """(entrance huts) the middle of the 3 x 1 floor opening, in the hut's floor layer; richting = where the lift lets you off."""
        self._jigsaw(x, y, z, f"down_{richting}", INGANG_BOVEN)

    def verbinding(self, x, y, z, facing, naam, doel="minecraft:empty", pool="minecraft:empty", final="minecraft:air"):
        """A horizontal jigsaw connector (facing: out of this piece)."""
        self._jigsaw(x, y, z, f"{facing}_up", naam, doel, pool, final, joint="aligned")

    def sign(self, x, y, z, facing, lines, wall=True, wood="birch", glow=False):
        """A waxed sign with up to four Dutch lines (each at most 16 characters), translatable through sign_text."""
        for line in lines:
            if isinstance(line, str) and len(line) > 16:
                self.fouten.append(f"{self.naam}: bordje {(x, y, z)}: '{line}' is langer dan 16 tekens")
        nbt = {"id": "minecraft:sign", "is_waxed": ms.Byte(1),
               "front_text": {"messages": sign_text.messages(self.prefix, lines), "color": "black", "has_glowing_text": ms.Byte(1 if glow else 0)},
               "back_text": {"messages": sign_text.messages(self.prefix, ["", "", "", ""]), "color": "black", "has_glowing_text": ms.Byte(0)}}
        if wall:
            self.set(x, y, z, f"minecraft:{wood}_wall_sign", {"facing": facing, "waterlogged": "false"}, nbt)
        else:
            self.set(x, y, z, f"minecraft:{wood}_sign", {"rotation": ROT[facing], "waterlogged": "false"}, nbt)

    def npc(self, x, y, z, plek, yaw=0.0, kind=None):
        """A talking guh (a plek of OnderWolBouw.PRAAT): it stands on the floor at (x, y, z)."""
        kind = kind or PRAAT_KIND[plek]
        self.entity(x + 0.5, y, z + 0.5, {"id": "guhs:guh_npc", "Kind": kind, "PersistenceRequired": ms.Byte(1), "Invulnerable": ms.Byte(1),
                                          "Rotation": ms.floats(float(yaw), 0.0), "RoleData": {"guhs_plek": plek}})

    def mijnguh(self, x, y, z, yaw=0.0, plek="mijnguhdorp"):
        """The Mijnguh (GuhNpcEntity.Kind.MIJNGUH) of the Mijnguhdorp: the spot of the Mijnguh-hulpje quest (RoleData
        guhs_plek, NeoForgeData guhs_bezetting "onderwol_mijnguh" for a Bezetting.npc repair)."""
        self.entity(x + 0.5, y, z + 0.5, {"id": "guhs:guh_npc", "Kind": "mijnguh", "PersistenceRequired": ms.Byte(1), "Invulnerable": ms.Byte(1),
                                          "Rotation": ms.floats(float(yaw), 0.0), "RoleData": {"guhs_plek": plek},
                                          "NeoForgeData": {"guhs_bezetting": "onderwol_mijnguh"}})

    def guh(self, x, y, z, yaw=0.0, slaapt=False, schaal=1.0):
        """A wild guh at (x, y, z) (x, z may be .5); slaapt: curled up asleep (the Slaapgrot's sleeping guh)."""
        nbt = ms.guh_nbt(schaal)
        nbt["Rotation"] = ms.floats(float(yaw), 0.0)
        if slaapt:
            nbt["NeoForgeData"] = {"guhs_onderwol_slaapt": ms.Byte(1)}
        self.entity(float(x), float(y), float(z), nbt)

    def kist(self, x, y, z, facing, loot):
        self.set(x, y, z, "minecraft:chest", {"facing": facing, "type": "single", "waterlogged": "false"}, {"id": "minecraft:chest", "LootTable": loot})

    def ton(self, x, y, z, loot, facing="up"):
        self.set(x, y, z, "minecraft:barrel", {"facing": facing, "open": "false"}, {"id": "minecraft:barrel", "LootTable": loot})

    def save(self, name):
        verbind(self)
        self.fouten += deuren_vrij(self)
        assert not self.fouten, "\n".join(self.fouten)
        super().save(name)
        # (the same bytes every run: make_structures' gzip stamps the time)
        import gzip
        pad = os.path.join(ms.OUT, name + ".nbt")
        with open(pad, "rb") as f:
            data = gzip.decompress(f.read())
        with open(pad, "wb") as f:
            f.write(gzip.compress(data, mtime=0))
        self.locatie = "guhs:" + name


def _staat(b):
    naam, props, _ = b
    return naam + ("[" + ",".join(f"{k}={v}" for k, v in sorted(props.items())) + "]" if props else "")


# =====================================================================================================================
# connected shapes: a jigsaw piece is placed with a "known shape" (no neighbour updates), so fences, walls, panes, bars and
# fence gates keep exactly the state stored in the template. verbind() works their connections out from the template's
# own neighbours before saving (like Minecraft's FenceBlock / WallBlock / IronBarsBlock / FenceGateBlock would).
# =====================================================================================================================
# (the code lives in tools/verbindingen.py now: make_structures.Structure.save uses it for every template)
from verbindingen import (RES, RICHTINGEN, TEGEN, NIET_VOL, VOL_TOCH, UITZONDERING, VOL_OUDERS, _tag, soort, _houten,  # noqa: E402,F401
                          _model_vol, vol, _echt, stevig, _poort_dwars, verbind, verbind_blokken, slingers)


# =====================================================================================================================
# doors: nothing in front of or behind a door (a barrel in the doorway, a bench against it)
# =====================================================================================================================
DOORLOPEN = ("air", "carpet", "rail", "torch", "sign", "button", "pressure_plate", "flower", "tulip", "poppy", "dandelion", "grass", "fern",
             "sapling", "vine", "lichen", "lichtmos", "pluisbolletje", "roze_gras", "glitterrank", "deurmat", "tapijt", "kleed", "snow",
             "redstone_wire", "ladder", "glinsterpaddenstoel", "guhpaddenstoel", "wolkenlift", "wolkenstroom", "light", "mos", "bloem",
             "blaadjes", "petals", "leaf_litter", "_door", "deur", "fence_gate", "poort", "water")


def doorloopbaar(blok):
    if blok is None:
        return False
    naam = blok[0].split(":", 1)[1]
    if blok[0] == "minecraft:jigsaw":
        return True    # (a connector: another piece goes on there)
    if "_trapdoor" in naam or "luik" in naam:
        return blok[1].get("open") == "true" or blok[1].get("half") == "top"
    if "snow" in naam and naam != "snow":
        return False
    return any(t in naam for t in DOORLOPEN) and not naam.endswith("_block")


def deuren_vrij(b):
    """Every door (its lower half, except b.nepdeuren) has a free way in front of it and behind it (both its blocks high).
    Nothing stored there (the world's own block) becomes air: a doorway always gets its room. Returns the problems."""
    fouten = []
    for p, (naam, props, _) in list(b.blocks.items()):
        if not (naam.endswith("_door") or naam.endswith("deur")) or "trapdoor" in naam or props.get("half") != "lower" or p in b.nepdeuren:
            continue
        d = RICHTINGEN.get(props.get("facing"))
        if d is None:
            continue
        for k in (1, -1):
            for dy in (0, 1):
                q = (p[0] + k * d[0], p[1] + dy, p[2] + k * d[2])
                if not (0 <= q[0] < b.size[0] and 0 <= q[1] < b.size[1] and 0 <= q[2] < b.size[2]):
                    continue    # (outside the template: the world, or the next piece)
                blok = b.blocks.get(q)
                if blok is None:
                    b.set(*q, "minecraft:air")
                    continue
                if not doorloopbaar(blok):
                    fouten.append(f"{b.naam}: deur {p} ({props.get('facing')}): {q} is niet vrij: {blok[0] if blok else 'niets (de wereld)'}")
    return fouten


def processor_list(h):
    h.w(f"{h.D}/worldgen/processor_list/onderwol_grot_schil.json", {"processors": [{"processor_type": PROCESSORS}]})


def pool(h, pad, elementen, fallback="minecraft:empty"):
    h.w(f"{h.D}/worldgen/template_pool/{pad}.json", {"fallback": fallback, "elements": [
        {"weight": w, "element": {"element_type": "minecraft:single_pool_element", "location": b.locatie if isinstance(b, Bouw) else b,
                                  "projection": "rigid", "processors": PROCESSORS}} for b, w in elementen]})


def reikwijdte(templates, naam, zaal=None, jigsaw=None, ingang=False):
    """keep_clear: how far the pieces can reach from the middle of the start chunk (the anchor lands there)."""
    r = 0
    for b, _ in templates:
        ax, _, az = b.ankers[f"guhs:{naam}_midden"]
        sx, _, sz = b.size
        r = max(r, ax, sx - 1 - ax, az, sz - 1 - az)
    if jigsaw:
        r = max(r, jigsaw + 24)
    if zaal:
        r += zaal.get("rand", 4) + int(3 * zaal.get("ruw", 1.0) + 0.999) + 1
    if ingang:
        r += 8   # (the hut may stick out past the build: its marker lies inside the build, the hut is ~15 wide)
    return r + 2


def grot_bouw(h, naam, *, templates, titel, tooltip, salt, band, spacing=None, separation=None, gegarandeerd=None, ruimte="beide",
              zaal=None, ingang=None, dekking=6, size=1, max_distance=64, voorrang=None, pools=None, kompas="ondergrond", biomes=None,
              titel_en=None, tooltip_en=None):
    """One underground building: its structure, pools, sets, biome tag and Superkompas texts (see the module text)."""
    assert spacing is not None or gegarandeerd is not None, f"{naam}: no normal set and no guaranteed copy"
    assert ruimte in ("beide", "grot", "zaal"), ruimte
    D = h.D
    processor_list(h)
    pool(h, f"onderwol/{naam}/start", templates)
    for pnaam, elementen in (pools or {}).items():
        if pnaam.endswith("_eind"):
            continue
        eind = f"guhs:onderwol/{naam}/{pnaam}_eind" if f"{pnaam}_eind" in (pools or {}) else "minecraft:empty"
        pool(h, f"onderwol/{naam}/{pnaam}", elementen, fallback=eind)
    for pnaam, elementen in (pools or {}).items():
        if pnaam.endswith("_eind"):
            pool(h, f"onderwol/{naam}/{pnaam}", elementen)
    tag = f"#guhs:has_structure/{naam}"
    s = {"type": TYPE, "biomes": tag, "step": STEP, "spawn_overrides": {}, "terrain_adaptation": "none",
         "start_pool": f"guhs:onderwol/{naam}/start", "start_jigsaw_name": f"guhs:{naam}_midden", "size": size, "max_distance": max_distance,
         "min_y": band[0], "max_y": band[1], "dekking": dekking, "ruimte": ruimte,
         "keep_clear": reikwijdte(templates, naam, zaal, max_distance if size > 1 else None, bool(ingang))}
    if zaal:
        s["zaal"] = dict(zaal)
    if ingang:
        g = {"pool": INGANG_POOL}
        g.update(ingang)
        if g.setdefault("pogingen", POGINGEN if g.get("kans", 1.0) >= 1.0 else 1) > 1:
            s["keep_clear"] += SCHUIF_MAX
        else:
            del g["pogingen"]
        s["ingang"] = g
    if voorrang is not None:
        s["voorrang"] = voorrang
    h.w(f"{D}/worldgen/structure/{naam}.json", s)
    entry = [{"structure": f"guhs:{naam}", "weight": 1}]
    normaal = f"{D}/worldgen/structure_set/{naam}.json"
    if spacing is not None:
        assert separation is not None and separation < spacing, f"{naam}: separation < spacing"
        h.w(normaal, {"structures": entry, "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": separation, "salt": salt}})
    elif os.path.exists(normaal):
        os.remove(normaal)
    gg = f"{D}/worldgen/structure_set/{naam}_gegarandeerd.json"
    if gegarandeerd is not None:
        g = dict(gegarandeerd)
        h.w(gg, {"structures": entry, "placement": {"type": "guhs:gegarandeerd", "salt": salt + 7, "min_afstand": g.get("min", 250),
                                                    "max_afstand": g.get("max", 1000), "sector": g.get("sector", 0), "sectoren": g.get("sectoren", 1),
                                                    "alleen_nieuw": True}})
    elif os.path.exists(gg):
        os.remove(gg)
    h.w(f"{D}/tags/worldgen/biome/has_structure/{naam}.json", {"values": biomes or guhmension_biomes(h)})
    h.lang(f"structure.guhs.{naam}", titel_en or titel, titel)
    h.lang(f"structure.guhs.{naam}.tooltip", tooltip_en or tooltip, tooltip)
    # (a full make_resources run: make_v2.bouwruimte() keeps this keep_clear and voorrang)
    if hasattr(h, "RUIMTE_HOOKS"):
        h.RUIMTE_HOOKS[TYPE] = lambda js, jigsaw_reach: js.get("keep_clear")
    if hasattr(h, "VOORRANG"):
        if voorrang is not None:
            h.VOORRANG[naam] = voorrang
        else:
            h.VOORRANG.pop(naam, None)
    STRUCTUREN[naam] = dict(s=s, kompas=kompas, salt=salt, spacing=spacing, gegarandeerd=gegarandeerd)


def loot(h, naam, pools):
    """data/guhs/loot_table/chests/onderwol/<naam>.json: pools = [(rolls or (min, max), [(item, weight, min, max), ...])];
    item "leeg" = nothing (an empty entry with that weight)."""
    out = []
    for rolls, entries in pools:
        r = rolls if isinstance(rolls, int) else {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}
        es = []
        for item, wgt, lo, hi in entries:
            if item == "leeg":
                es.append({"type": "minecraft:empty", "weight": wgt})
                continue
            e = {"type": "minecraft:item", "name": item if ":" in item else f"guhs:{item}", "weight": wgt}
            if (lo, hi) != (1, 1):
                e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
            es.append(e)
        out.append({"rolls": r, "entries": es})
    h.w(f"{h.D}/loot_table/chests/onderwol/{naam}.json", {"type": "minecraft:chest", "pools": out})
    return f"guhs:chests/onderwol/{naam}"
