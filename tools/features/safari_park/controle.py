"""
The park check (test round 2): finds what breaks or looks broken when the park is stamped, and repairs what it safely can.
Generic: it looks at the finished volume (after terrein.py AND every zone module), never at hardcoded spots.

    herstel(park, log)    repair + report, called by kit.Park.schrijf right before the tiles are written
    rapport(park)         only count (dict per category), changes nothing

Categories:
  hekgat       fence-line columns of the park fences (park.hek) without any fence/post/gate, while both line neighbours have
               one (a hole, not a planned opening); repaired with a fence + mesh column
  verbinding   fences / walls / panes / bars / gates whose stored connections differ from what the game computes
               (gates: facing across the line, in_wall next to walls, closed); repaired
  blad         leaves that could decay (persistent=false): they would drop saplings and sticks; repaired
  steun        blocks that need support (plants, carpets, snow, torches, lanterns, signs, ladders, vines, lily pads,
               double plants / doors with a missing half) and have none: the game would break them (items on the ground);
               removed
  lek          water (and waterlogged blocks) next to or above something water flows into (air, plants, torches...):
               water would run out and wash things away; repaired with a bank block (or un-waterlogged)
  gat          terrain columns whose underside can be seen from a lower neighbour (you look into the void); filled down
  zweef        small floating bits (tree pieces, plants) not connected to the terrain; removed when only plants/wood,
               else reported
  padsprong    neighbouring path columns whose walking heights differ > 1 block; the lower one is raised
  void         columns near the park that are empty or end below y 20 (void view / falling off); filled
  plaat        big unnatural flat regions (one height + one top block, >= 300 columns, outside the entreedorp); reported
  ingegraven   columns with a log / leaves right under terrain ground (a buried tree); reported per zone
  doorgang     clearance along the rides (truck route, kano, boot, kabelbaan): leaves/plants removed, rest reported
  kuil         (round 4) a dry 1x1 pit >= 2 deep in walkable ground (you fall in); filled up
  bereik       (round 4) doors, guh chairs, viewpoints, geheimpjes and jetties a player cannot WALK to from the entrance
               square (step <= 1, drop <= 3, 2 high, doors/gates/ladders/swimming); reported with coordinates
  kano         river columns (water at the surface y 62, river or lakes) with a block 1-3 above the water: the canoe
               cannot pass under; reported (bridges are built with 3 free blocks by terrein.route)
"""
from functools import lru_cache

import numpy as np
from scipy.ndimage import binary_dilation, label

from . import kit
from .kit import W, H, WATER_Y, state, split_state

# --- block classes (by name) ------------------------------------------------------------------------------------------
_PLANT_WOORDEN = ("short_grass", "tall_grass", "fern", "dead_bush", "sapling", "flower", "tulip", "daisy", "allium",
                  "bluet", "orchid", "dandelion", "poppy", "cornflower", "lily_of_the_valley", "rose_bush", "peony",
                  "lilac", "sunflower", "petals", "torchflower", "pitcher", "wither_rose", "sweet_berry",
                  "brown_mushroom", "red_mushroom", "crimson_fungus", "warped_fungus", "crimson_roots", "warped_roots",
                  "nether_sprouts", "bush", "azalea", "dripleaf", "spore_blossom", "hanging_roots", "glow_lichen",
                  "sugar_cane", "bamboo_sapling", "cactus_flower", "firefly_bush", "leaf_litter", "short_dry_grass",
                  "tall_dry_grass", "wildflowers", "eyeblossom")
_DUBBEL = {"tall_grass", "large_fern", "sunflower", "lilac", "rose_bush", "peony", "tall_seagrass", "pitcher_plant",
           "small_dripleaf", "tall_dry_grass"}
_GROND_PLANT = ("grass_block", "dirt", "coarse_dirt", "podzol", "rooted_dirt", "moss_block", "mud", "muddy_mangrove_roots",
                "mycelium", "farmland", "pale_moss_block")
_ZAND = ("sand", "red_sand", "suspicious_sand")


def _naam(s):
    return split_state(s)[0].split(":", 1)[1] if ":" in split_state(s)[0] else split_state(s)[0]


def _ns(s):
    return split_state(s)[0].split(":", 1)[0] if ":" in split_state(s)[0] else "minecraft"


@lru_cache(maxsize=None)
def soort(s):
    """A rough class of a block state string."""
    if s == "minecraft:air":
        return "lucht"
    ns, n = _ns(s), _naam(s)
    props = split_state(s)[1]
    if n in ("water", "bubble_column", "seagrass", "kelp", "kelp_plant") or n == "tall_seagrass":
        return "water"
    if n == "lava":
        return "lava"
    if n == "safaripoort":
        return "grote_poort"      # the guhs safari gate (4x6 doors): it opens for the truck and for players, so a path
                                  # and a ride pass it; never a fence gate (verbindingen must not touch its state)
    if n.endswith("_fence_gate") or n.startswith("guh_poortje"):
        return "poort"
    if n.endswith("_fence") or n.startswith("guh_hekje"):          # (the bouwhulp's guh fences are fences too)
        return "hek"
    if n.endswith("_pane") or n == "iron_bars":
        return "ruit"
    if n.endswith("_wall") and ns == "minecraft":
        return "muur"
    if n.endswith("_leaves"):
        return "blad"
    if n == "lily_pad":
        return "lelie"
    if n == "vine":
        return "rank"
    if n in ("ladder",) or n.endswith(("_wall_sign", "_wall_banner", "wall_torch", "_wall_hanging_sign")) \
            or n.endswith("_button") or n == "lever" or n.endswith("_wall_fan") or n == "tripwire_hook":
        return "wand"
    if n in ("lantern", "soul_lantern") or n.endswith("_hanging_sign"):
        return "hangend" if props.get("hanging") == "true" or n.endswith("_hanging_sign") else "staand"
    if n in ("torch", "soul_torch", "redstone_torch") or n.endswith("_sign") or n.endswith("_banner") \
            or n.endswith("_carpet") or n == "moss_carpet" or n.endswith("_pressure_plate") or n.endswith("rail") \
            or n == "snow" or n.endswith("candle") or n == "redstone_wire" or n == "sea_pickle" or n.endswith("_coral") \
            or n.endswith("_coral_fan") or n == "flower_pot" or n.startswith("potted_") \
            or n == "decorated_pot" or n == "campfire" or n == "chain":
        return "staand"
    if n == "bamboo":
        return "plant"
    if any(w in n for w in _PLANT_WOORDEN) or n in _DUBBEL or n == "large_fern":
        return "plant"
    if n.endswith("_door"):
        return "deur"
    return "vast"


@lru_cache(maxsize=None)
def _vol(s):
    """Does a fence/pane/wall connect to this block's side (a sturdy full face, roughly)?"""
    k = soort(s)
    if k != "vast":
        return False
    ns, n = _ns(s), _naam(s)
    if ns != "minecraft":
        return False             # modded blocks: unknown shapes, never connect
    niet = ("slab", "stairs", "door", "chest", "cake", "bed", "skull", "head", "anvil", "bell", "lectern", "grindstone",
            "stonecutter", "hopper", "cauldron", "brewing", "enchanting", "daylight", "composter", "end_rod", "lightning",
            "mangrove_roots", "chorus", "cactus", "dirt_path", "farmland", "scaffolding", "cobweb", "pointed_dripstone",
            "amethyst_cluster", "_bud", "turtle_egg", "sniffer_egg", "frogspawn", "conduit", "heavy_core", "trial_spawner",
            "vault", "honey_block", "soul_sand", "mud", "snow_layer", "trapdoor", "barrier", "light", "structure_void")
    if any(w in n for w in niet):
        if n == "muddy_mangrove_roots" or (n.endswith("_slab") and split_state(s)[1].get("type") == "double"):
            return True
        return False
    return True


def _vloer_ok(s):
    """Something a torch / carpet / sign can stand on (anything solid-ish)."""
    return soort(s) in ("vast", "muur", "hek", "poort", "blad", "ruit") and soort(s) != "lucht"


class _Lut:
    """Per palette id: class, full, ... (grows with the palette)."""

    def __init__(self, park):
        self.park = park
        self.n = 0
        self.kl = []
        self.vol = []
        self.ververs()

    def ververs(self):
        for i in range(self.n, len(self.park.pal)):
            s = self.park.pal[i]
            self.kl.append(soort(s))
            self.vol.append(_vol(s))
        self.n = len(self.park.pal)

    def arr(self, pred):
        self.ververs()
        return np.array([pred(i) for i in range(self.n)], bool)


# ======================================================================================================================
def blad(park, herstel):
    """Leaves never decay: persistent=true on every leaves state."""
    lut = np.arange(len(park.pal), dtype=np.uint16)
    n_fout = 0
    for i, s in enumerate(list(park.pal)):
        if soort(s) == "blad" and split_state(s)[1].get("persistent") != "true":
            name, props = split_state(s)
            props["persistent"] = "true"
            lut[i] = park.id(state(name, **props))
    fout_ids = np.nonzero(lut != np.arange(len(lut)))[0]
    if len(fout_ids):
        mask = np.isin(park.v, fout_ids)
        n_fout = int(mask.sum())
        if herstel:
            full = np.arange(len(park.pal), dtype=np.uint16)
            full[:len(lut)] = lut
            park.v[mask] = full[park.v[mask]]
    return n_fout


# --- fences ------------------------------------------------------------------------------------------------------------
_RICHTING = (("north", 0, -1), ("south", 0, 1), ("east", 1, 0), ("west", -1, 0))


def _verbindt(park, kl_self, s_self, nb):
    """Would a block of class kl_self connect to neighbour state nb (in that direction)? Gate axis handled by caller."""
    k = soort(nb)
    if kl_self == "hek":
        if k == "hek":
            # wooden fences connect to each other, the nether brick fence only to itself
            return ("nether_brick" in _naam(nb)) == ("nether_brick" in _naam(s_self))
        return k == "poort" or _vol(nb)
    if kl_self == "ruit":
        return k in ("ruit", "muur") or _vol(nb)
    if kl_self == "muur":
        return k in ("muur", "ruit", "poort") or _vol(nb)
    return False


def _poort_as(s):
    """'x' when the gate spans x (facing north/south), 'z' when it spans z."""
    f = split_state(s)[1].get("facing", "north")
    return "x" if f in ("north", "south") else "z"


def verbindingen(park, herstel):
    """Fences, walls, panes, bars and gates: the connections the game would compute (and gates set across their line)."""
    kl = np.array([soort(s) for s in park.pal])
    ids = np.nonzero(np.isin(kl, ["hek", "ruit", "muur", "poort"]))[0]
    if not len(ids):
        return 0
    xs, ys, zs = np.nonzero(np.isin(park.v, ids))
    fout = 0
    # gates first (their facing decides whether fences connect to them)
    for x, y, z in zip(xs, ys, zs):
        s = park.pal[park.v[x, y, z]]
        if soort(s) != "poort":
            continue
        name, props = split_state(s)
        lijn_x = any(soort(park.get(x + dx, y, z)) in ("hek", "muur", "ruit") or soort(park.get(x + dx, y, z)) == "poort"
                     for dx in (-1, 1))
        lijn_z = any(soort(park.get(x, y, z + dz)) in ("hek", "muur", "ruit") or soort(park.get(x, y, z + dz)) == "poort"
                     for dz in (-1, 1))
        if lijn_x and not lijn_z:
            facing = props.get("facing", "north") if props.get("facing") in ("north", "south") else "north"
        elif lijn_z and not lijn_x:
            facing = props.get("facing", "east") if props.get("facing") in ("east", "west") else "east"
        else:
            facing = props.get("facing", "north")
        as_ = "x" if facing in ("north", "south") else "z"
        buren = [park.get(x + dx, y, z) for dx in (-1, 1)] if as_ == "x" else [park.get(x, y, z + dz) for dz in (-1, 1)]
        in_wall = "true" if any(soort(b) == "muur" for b in buren) else "false"
        nieuw = dict(props, facing=facing, in_wall=in_wall, open="false", powered="false")
        if nieuw != props:
            fout += 1
            if herstel:
                park.v[x, y, z] = park.id(state(name, **nieuw))
    for x, y, z in zip(xs, ys, zs):
        s = park.pal[park.v[x, y, z]]
        k = soort(s)
        if k == "poort":
            continue
        name, props = split_state(s)
        nieuw = dict(props)
        aantal = {}
        for richting, dx, dz in _RICHTING:
            nb = park.get(x + dx, y, z)
            ok = _verbindt(park, k, s, nb)
            if ok and soort(nb) == "poort":
                ok = (_poort_as(nb) == "x") == (dx != 0)    # a gate spanning x connects to fences east/west of it
            aantal[richting] = ok
            if k == "muur":
                nieuw[richting] = ("tall" if soort(park.get(x, y + 1, z)) in ("muur", "vast") and ok else "low") if ok else "none"
            else:
                nieuw[richting] = "true" if ok else "false"
        if k == "muur":
            recht = (aantal["north"] and aantal["south"] and not aantal["east"] and not aantal["west"]) or \
                    (aantal["east"] and aantal["west"] and not aantal["north"] and not aantal["south"])
            boven = soort(park.get(x, y + 1, z))
            nieuw["up"] = "false" if recht and boven not in ("muur", "staand") else "true"
        if "waterlogged" in props or k in ("hek", "ruit", "muur"):
            nieuw.setdefault("waterlogged", props.get("waterlogged", "false"))
        if nieuw != props:
            fout += 1
            if herstel:
                park.v[x, y, z] = park.id(state(name, **nieuw))
    return fout


def hekgaten(park, herstel):
    """Holes in the park fences: a fence-line column without fence/post/gate whose line neighbours both have one."""
    if getattr(park, "hek", None) is None:
        return 0
    kl = np.array([soort(s) for s in park.pal])
    hekachtig = np.isin(kl, ["hek", "ruit", "muur", "poort", "grote_poort"])
    paal = np.array([s.startswith("minecraft:dark_oak_log") for s in park.pal])
    heeft = np.zeros((W, W), bool)
    for y in range(40, H):
        laag = park.v[:, y, :]
        heeft |= hekachtig[laag] | paal[laag]
    pad_ids = [i for i, s in enumerate(park.pal) if _naam(s) in ("dirt_path", "spruce_planks", "spruce_slab", "gravel",
                                                                      "smooth_sandstone", "cut_sandstone")]
    lijn = park.hek & ~park.water
    gaten = []
    xs, zs = np.nonzero(lijn & ~heeft)
    for x, z in zip(xs, zs):
        buren = [(x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))
                 if 0 <= x + dx < W and 0 <= z + dz < W and lijn[x + dx, z + dz]]
        if len(buren) < 2 or not all(heeft[bx, bz] for bx, bz in buren):
            continue
        top = park.top(x, z)
        if top >= 0 and park.v[x, top, z] in pad_ids:
            continue                      # a path goes through: a planned opening (the zone builds its gate)
        gaten.append((x, z))
    if herstel:
        post = state("dark_oak_log", axis="y")
        for x, z in gaten:
            top = park.top(x, z)
            while top > 0 and soort(park.get(x, top, z)) in ("plant", "staand", "blad"):
                park.v[x, top, z] = 0
                top -= 1
            buiten = bool(getattr(park, "hek_buiten", np.zeros((W, W), bool))[x, z])
            hoog = 4 if buiten else 3
            if (x * 7 + z * 3) % 4 == 0:
                for y in range(top + 1, top + hoog + 2):
                    park.set(x, y, z, post)
            else:
                park.set(x, top + 1, z, "minecraft:dark_oak_fence")
                for y in range(top + 2, top + hoog + 1):
                    park.set(x, y, z, "minecraft:iron_bars")
    return len(gaten)


# --- support -------------------------------------------------------------------------------------------------------------
def _plant_grond(plant, onder):
    p, o = _naam(plant), _naam(onder)
    if soort(onder) == "plant" and (p == o or (p in ("bamboo",) and o == "bamboo_sapling")):
        return True                         # stacks (bamboo, sugar cane)
    if p in ("brown_mushroom", "red_mushroom"):
        # in daylight a mushroom only survives on podzol / mycelium / nylium (steun() turns grass under it into podzol)
        return o in ("podzol", "mycelium", "crimson_nylium", "warped_nylium")
    if p == "dead_bush" or p.endswith("dry_grass"):
        return o in _GROND_PLANT or o in _ZAND or "terracotta" in o
    if p == "sugar_cane":
        return o in _GROND_PLANT or o in _ZAND
    if p in ("bamboo", "bamboo_sapling"):
        return o in _GROND_PLANT or o in _ZAND or o in ("gravel", "suspicious_gravel")
    if p in ("hanging_roots", "spore_blossom"):
        return True                         # hang from above: checked separately
    if p == "glow_lichen":
        return True
    if p in ("azalea", "flowering_azalea"):
        return o in _GROND_PLANT or o == "clay"
    if p == "sweet_berry_bush":
        return o in _GROND_PLANT
    return o in _GROND_PLANT


def steun(park, herstel):
    """Blocks without the support they need (the game would break them, dropping items)."""
    kl = np.array([soort(s) for s in park.pal])
    ids = np.nonzero(np.isin(kl, ["plant", "staand", "hangend", "wand", "rank", "lelie", "deur"])
                     | np.array([_naam(s) in ("seagrass", "kelp", "kelp_plant", "tall_seagrass") for s in park.pal]))[0]
    weg = []
    pas_aan = []
    xs, ys, zs = np.nonzero(np.isin(park.v, ids))
    for x, y, z in zip(xs, ys, zs):
        s = park.pal[park.v[x, y, z]]
        k = soort(s)
        n = _naam(s)
        props = split_state(s)[1]
        onder = park.get(x, y - 1, z) if y > 0 else "minecraft:air"
        boven = park.get(x, y + 1, z)
        if k == "water":
            if n in ("seagrass", "kelp", "kelp_plant", "tall_seagrass"):
                if n == "tall_seagrass" and props.get("half") == "upper":
                    if _naam(onder) != "tall_seagrass":
                        weg.append((x, y, z, "minecraft:water[level=0]"))
                    continue
                if n == "kelp_plant" or (n == "kelp" and _naam(onder) in ("kelp", "kelp_plant")):
                    if _naam(onder) not in ("kelp", "kelp_plant") and soort(onder) != "vast":
                        weg.append((x, y, z, "minecraft:water[level=0]"))
                    continue
                if soort(onder) != "vast":
                    weg.append((x, y, z, "minecraft:water[level=0]"))
            continue
        if n in _DUBBEL or n == "large_fern" or k == "deur":
            half = props.get("half")
            if half == "upper":
                if _naam(onder) != n:
                    weg.append((x, y, z, None))
                continue
            if half == "lower" and _naam(boven) != n:
                weg.append((x, y, z, None))
                continue
        if k == "plant":
            if n in ("hanging_roots", "spore_blossom"):
                if soort(boven) not in ("vast", "blad"):
                    weg.append((x, y, z, None))
            elif n == "glow_lichen":
                pass
            elif n in _DUBBEL and props.get("half") == "upper":
                pass
            elif n in ("brown_mushroom", "red_mushroom") and not _plant_grond(s, onder) \
                    and _naam(onder) in ("grass_block", "dirt", "coarse_dirt", "rooted_dirt", "moss_block"):
                pas_aan.append((x, y - 1, z, state("podzol", snowy="false")))
            elif not _plant_grond(s, onder):
                weg.append((x, y, z, None))
            elif n == "sugar_cane" and _naam(onder) != "sugar_cane":
                nat = any(soort(park.get(x + dx, y - 1, z + dz)) == "water" or
                          split_state(park.get(x + dx, y - 1, z + dz))[1].get("waterlogged") == "true"
                          for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                if not nat:
                    weg.append((x, y, z, None))
        elif k == "staand":
            if n == "snow":
                if _naam(onder) in ("ice", "packed_ice", "blue_ice", "barrier") or not _vloer_ok(onder) or soort(onder) == "blad" and False:
                    weg.append((x, y, z, None))
            elif n == "chain":
                pass
            elif not _vloer_ok(onder) and not (n.endswith("_carpet") and soort(onder) != "lucht" and soort(onder) != "water"):
                weg.append((x, y, z, None))
        elif k == "hangend":
            if soort(boven) not in ("vast", "hek", "muur", "ruit", "blad") and _naam(boven) != "chain" \
                    and not _naam(boven).endswith(("_fence", "_wall")):
                weg.append((x, y, z, None))
        elif k == "wand":
            f = props.get("facing")
            if n.endswith("_button") or n == "lever":
                face = props.get("face", "wall")
                if face == "floor":
                    ok = soort(onder) == "vast"
                elif face == "ceiling":
                    ok = soort(boven) == "vast"
                else:
                    dx, dz = {"north": (0, 1), "south": (0, -1), "east": (-1, 0), "west": (1, 0)}.get(f, (0, 0))
                    ok = soort(park.get(x + dx, y, z + dz)) == "vast"
            else:
                dx, dz = {"north": (0, 1), "south": (0, -1), "east": (-1, 0), "west": (1, 0)}.get(f, (0, 0))
                ok = soort(park.get(x + dx, y, z + dz)) == "vast"      # (a wall thing needs a solid wall block)
            if not ok:
                weg.append((x, y, z, None))
        elif k == "rank":
            name, p2 = split_state(s)
            nieuw = dict(p2)
            iets = False
            for richting, dx, dz in _RICHTING:
                if p2.get(richting) == "true":
                    ok = soort(park.get(x + dx, y, z + dz)) in ("vast", "blad")
                    nieuw[richting] = "true" if ok else "false"
                    iets |= ok
            if p2.get("up") == "true":
                ok = soort(boven) in ("vast", "blad")
                nieuw["up"] = "true" if ok else "false"
                iets |= ok
            if not iets:
                # hanging from a vine above keeps it alive
                if _naam(boven) == "vine":
                    continue
                weg.append((x, y, z, None))
            elif nieuw != p2:
                pas_aan.append((x, y, z, state(name, **nieuw)))
        elif k == "lelie":
            if soort(onder) != "water" or _naam(onder) != "water":
                weg.append((x, y, z, None))
    if herstel:
        for x, y, z, vervang in weg:
            park.v[x, y, z] = park.id(vervang) if vervang else 0
        for x, y, z, s in pas_aan:
            park.v[x, y, z] = park.id(s)
    return len(weg) + len(pas_aan)


# --- water ----------------------------------------------------------------------------------------------------------------
def _water_masks(park):
    kl = np.array([soort(s) for s in park.pal])
    nat_id = (kl == "water") | np.array([split_state(s)[1].get("waterlogged") == "true" for s in park.pal])
    zacht_id = np.isin(kl, ["lucht", "plant", "staand", "rank", "lelie"]) & ~nat_id
    # lily pads float on water: water may stand under them, but water does not flow into them sideways (they are
    # placed above the surface); treat them as soft only for the cell they are in (sideways), which is fine
    return nat_id, zacht_id


def lekken(park, herstel, max_rondes=6):
    """Water next to (or above) something it flows into. Repaired with a bank block; a leaking waterlogged block is
    un-waterlogged instead. Repeated until nothing leaks (a bank block can uncover the next leak)."""
    totaal = 0
    for ronde in range(max_rondes):
        nat_id, zacht_id = _water_masks(park)
        nat = nat_id[park.v]
        zacht = zacht_id[park.v]
        # waterfalls (a column of water above the water line) are meant to fall: not a leak
        val = np.zeros_like(nat)
        val[:, 1:-1, :] = nat[:, 1:-1, :] & (nat[:, 2:, :] | nat[:, :-2, :])
        val[:, :WATER_Y + 2, :] = False
        nat = nat & ~val
        lek = np.zeros_like(nat)
        doel = np.zeros_like(nat)                  # the soft cells water would run into
        # sideways
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nb_zacht = np.zeros_like(zacht)
            if dx == 1:
                nb_zacht[:-1] = zacht[1:]
            elif dx == -1:
                nb_zacht[1:] = zacht[:-1]
            elif dz == 1:
                nb_zacht[:, :, :-1] = zacht[:, :, 1:]
            else:
                nb_zacht[:, :, 1:] = zacht[:, :, :-1]
            m = nat & nb_zacht
            lek |= m
            if dx == 1:
                doel[1:] |= m[:-1]
            elif dx == -1:
                doel[:-1] |= m[1:]
            elif dz == 1:
                doel[:, :, 1:] |= m[:, :, :-1]
            else:
                doel[:, :, :-1] |= m[:, :, 1:]
        # down
        onder_zacht = np.zeros_like(zacht)
        onder_zacht[:, 1:, :] = zacht[:, :-1, :]
        m = nat & onder_zacht
        lek |= m
        doel[:, :-1, :] |= m[:, 1:, :]
        # the world edge (x/z 0 or W-1): water there runs out of the stamped box
        rand = np.zeros_like(nat)
        rand[0], rand[-1], rand[:, :, 0], rand[:, :, -1] = nat[0], nat[-1], nat[:, :, 0], nat[:, :, -1]
        n = int(lek.sum()) + int(rand.sum())
        if ronde == 0:
            totaal = n
        if n == 0 or not herstel:
            break
        # waterlogged blocks that leak: dry them (a sunken fence or plank at the bank)
        wl_ids = [i for i, s in enumerate(park.pal) if split_state(s)[1].get("waterlogged") == "true"
                  and soort(s) != "water"]
        if wl_ids:
            wl = np.isin(park.v, wl_ids) & lek
            for x, y, z in zip(*np.nonzero(wl)):
                name, props = split_state(park.pal[park.v[x, y, z]])
                props["waterlogged"] = "false"
                park.v[x, y, z] = park.id(state(name, **props))
        # soft cells water runs into: a bank block like the ground around it
        for x, y, z in zip(*np.nonzero(doel)):
            park.v[x, y, z] = park.id(_oever_blok(park, x, y, z))
        for x, y, z in zip(*np.nonzero(rand)):
            park.v[x, y, z] = park.id("minecraft:stone")
    return totaal


def _oever_blok(park, x, y, z):
    """A bank block fitting the spot: the ground block of the zone (top) or its under-ground."""
    zv = getattr(park, "zone_vol", None)
    from .terrein import ZONES, OPP, _blok
    k = ZONES[int(zv[x, z])] if zv is not None else "europa"
    tops, sub, bed = OPP[k]
    boven = park.get(x, y + 1, z)
    if soort(boven) in ("lucht", "plant", "staand"):
        return _blok(tops[0][0])
    return _blok(sub)


# --- gravity ------------------------------------------------------------------------------------------------------------
_VAL = {"sand": "sandstone", "red_sand": "red_sandstone", "gravel": "andesite", "suspicious_sand": "sandstone",
        "suspicious_gravel": "andesite"}


def vallen(park, herstel):
    """Sand / gravel / concrete powder with nothing solid under it falls (and breaks what it lands on): made solid."""
    kl = np.array([soort(s) for s in park.pal])
    zacht = np.isin(kl, ["lucht", "plant", "staand", "rank", "lelie", "water"])
    val_ids = [i for i, s in enumerate(park.pal) if _naam(s) in _VAL or _naam(s).endswith("_concrete_powder")]
    if not val_ids:
        return 0
    m = np.isin(park.v, val_ids)
    onder = np.zeros_like(m)
    onder[:, 1:, :] = zacht[park.v[:, :-1, :]]
    m &= onder
    n = int(m.sum())
    if herstel:
        for x, y, z in zip(*np.nonzero(m)):
            nm = _naam(park.pal[park.v[x, y, z]])
            vast = _VAL.get(nm) or nm.replace("_concrete_powder", "_concrete")
            park.v[x, y, z] = park.id("minecraft:" + vast)
    return n


# --- holes / floating -----------------------------------------------------------------------------------------------------
def gaten(park, herstel):
    """Columns whose underside shows: a 4-neighbour's top lies below this column's lowest block - 1."""
    vast = park.v != 0
    heeft = vast.any(axis=1)
    laag = np.where(heeft, np.argmax(vast, axis=1), H)                          # lowest non-air y
    top = np.where(heeft, H - 1 - np.argmax(vast[:, ::-1, :], axis=1), -1)      # highest non-air y
    min_bur = np.full((W, W), H)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        t = np.full((W, W), -1)
        if dx == 1:
            t[:-1] = top[1:]
        elif dx == -1:
            t[1:] = top[:-1]
        elif dz == 1:
            t[:, :-1] = top[:, 1:]
        else:
            t[:, 1:] = top[:, :-1]
        t = np.where(t < 0, H, t)            # a neighbour outside the box / empty: does not count
        min_bur = np.minimum(min_bur, t)
    gat = heeft & (min_bur < laag - 1)
    leeg = ~heeft
    n = int(gat.sum()) + int(leeg.sum())
    if herstel:
        stone = park.id("minecraft:stone")
        for x, z in zip(*np.nonzero(gat)):
            for y in range(max(int(min_bur[x, z]) - 1, 1), int(laag[x, z])):
                park.v[x, y, z] = stone
        if leeg.any() and getattr(park, "hoogte", None) is not None:
            for x, z in zip(*np.nonzero(leeg)):
                g = int(park.hoogte[x, z])
                for y in range(max(g - 4, 1), g + 1):
                    park.v[x, y, z] = stone
    return n


def zweven(park, herstel, max_weg=400):
    """Pieces not connected to the terrain (the biggest connected piece). Small all-plant/wood pieces are removed."""
    lab, n = label(park.v != 0, structure=np.ones((3, 3, 3), bool))     # 26-connected: diagonal joints hold too
    if n <= 1:
        return 0, []
    grootte = np.bincount(lab.ravel())
    grootte[0] = 0
    hoofd = int(grootte.argmax())
    kl = np.array([soort(s) for s in park.pal])
    hout = np.array([s.split("[")[0].endswith(("_log", "_wood", "_leaves", "vine", "_planks")) for s in park.pal])
    weg_ok = np.isin(kl, ["plant", "blad", "rank", "staand", "lelie"]) | hout
    stukken = [i for i in range(1, n + 1) if i != hoofd and grootte[i] > 0]
    gerapporteerd = []
    verwijderd = 0
    if stukken:
        from scipy.ndimage import find_objects
        sl = find_objects(lab)
        for i in stukken:
            s = sl[i - 1]
            box = lab[s] == i
            ids = park.v[s][box]
            if grootte[i] <= max_weg and weg_ok[ids].all():
                if herstel:
                    sub = park.v[s]
                    sub[box] = 0
                verwijderd += 1
            else:
                gerapporteerd.append((s[0].start, s[1].start, s[2].start, int(grootte[i])))
    return len(stukken), gerapporteerd


def kano(park):
    """River / lake columns at the surface with something 1-2 blocks above the water (canoe cannot pass)."""
    nat_id, _ = _water_masks(park)
    opp = nat_id[park.v[:, WATER_Y, :]]
    if getattr(park, "water", None) is not None:
        opp &= park.water
    vrij = np.ones((W, W), bool)
    for y in range(WATER_Y + 1, WATER_Y + 3):     # 2 free blocks: a boat with a seated guh/player fits (deck at water + 3)
        laag = park.v[:, y, :]
        vrij &= (laag == 0) | np.isin(laag, [park.idx.get("minecraft:lily_pad", -1)])
    blok = opp & ~vrij
    for (x, z) in getattr(park, "steiger_cellen", None) or ():
        blok[x, z] = False                 # (round 4) a jetty of the boat line at the bank: meant to be there
    lab, n = label(blok)
    plekken = []
    if n:
        from scipy.ndimage import find_objects
        for i, s in enumerate(find_objects(lab), 1):
            xs, zs = np.nonzero(lab[s] == i)
            if len(xs) < 3:
                continue                   # a single plant / root / lantern over the water: not a bridge
            x, z = int(xs.mean() + s[0].start), int(zs.mean() + s[1].start)
            zone = "?"
            if getattr(park, "zone_vol", None) is not None:
                from .terrein import ZONES
                zone = ZONES[int(park.zone_vol[x, z])]
            plekken.append((x, z, len(xs), zone))
    return plekken



# --- round 3: plates, buried trees, path jumps, void, free passage ------------------------------------------------------
_GROND_WOORDEN = ("grass_block", "dirt", "coarse_dirt", "rooted_dirt", "podzol", "mud", "clay", "sand", "red_sand",
                  "gravel", "snow_block", "stone", "andesite", "diorite", "granite", "packed_ice", "red_sandstone",
                  "sandstone", "terracotta", "moss_block", "dirt_path", "tuff", "deepslate", "calcite")
_HOUT_BLAD = ("_log", "_leaves", "_wood")


def _grond_lut(park):
    return np.array([soort(s) == "vast" and _naam(s) in _GROND_WOORDEN for s in park.pal], bool)


def _boom_lut(park):
    return np.array([_naam(s).endswith(_HOUT_BLAD) for s in park.pal], bool)


def _bovenste(mask):
    """Highest y where mask[x, y, z] (-1 if none), per column."""
    heeft = mask.any(axis=1)
    return np.where(heeft, H - 1 - np.argmax(mask[:, ::-1, :], axis=1), -1)


def _zone_naam(park, x, z):
    from .terrein import ZONES
    zv = getattr(park, "zone_vol", None)
    return ZONES[int(zv[x, z])] if zv is not None else "?"


def platen(park, min_opp=300):
    """Big unnaturally flat areas: 4-connected land regions inside the park with one ground height AND one top block,
    bigger than min_opp columns (the old 'raise every dry column below the water line' fixes made these). The
    entreedorp (flat by design), paths and water are left out. Reported, not repaired (the cause is fixed instead)."""
    if getattr(park, "binnen", None) is None:
        return 0, []
    gl = _grond_lut(park)
    gm = gl[park.v]
    top = _bovenste(gm)
    ids = park.v[np.arange(W)[:, None], np.clip(top, 0, H - 1), np.arange(W)[None, :]]
    from .terrein import ZONES
    m = park.binnen & ~park.water & ~park.pad & (top > 0) & (park.zone != ZONES.index("entree"))
    sleutel = top.astype(np.int64) * 100000 + ids.astype(np.int64)
    gezien = np.zeros((W, W), bool)
    uit = []
    # a region = same key, 4-connected: label per distinct key among the masked cells (only keys that occur often)
    vals, tel = np.unique(sleutel[m], return_counts=True)
    for v in vals[tel >= min_opp]:
        lab, n = label(m & (sleutel == v))
        if not n:
            continue
        gr = np.bincount(lab.ravel())
        gr[0] = 0
        for i in np.nonzero(gr >= min_opp)[0]:
            xs, zs = np.nonzero(lab == i)
            x, z = int(xs.mean()), int(zs.mean())
            uit.append((x, z, int(gr[i]), _naam(park.pal[int(v % 100000)]), int(v // 100000), _zone_naam(park, x, z)))
    return len(uit), uit


def ingegraven(park, herstel=False):
    """Tree pieces (logs, leaves) with terrain ground right above them in the same column: a tree buried by ground that
    was raised later (a hill or a meadow built over it). Repaired: the buried wood/leaves become dirt, and what still
    sticks out of the new ground within 3 blocks of a buried column (a crown 'growing out of the hill') is removed."""
    gm = _grond_lut(park)[park.v]
    bm = _boom_lut(park)[park.v]
    onder_grond = np.zeros_like(bm)
    onder_grond[:, :-1, :] = gm[:, 1:, :]
    hit = (bm & onder_grond).any(axis=1)
    if herstel and hit.any():
        from scipy.ndimage import binary_dilation as _dil
        dirt = park.id("minecraft:dirt")
        top_grond = _bovenste(gm)
        for x, z in zip(*np.nonzero(hit)):
            col_b = bm[x, :, z]
            for y in np.nonzero(col_b)[0].tolist():
                if y < top_grond[x, z]:
                    park.v[x, y, z] = dirt
        rond = _dil(hit, iterations=3)
        # (round 4) only real tree columns lose their crown: a column with anything built in it (planks, fences, a
        # ladder, a lantern: a lookout tower's leg, a hut's corner post) is a building, never a buried tree's crown
        gebouwd = np.array([soort(s) not in ("lucht", "plant", "blad", "rank") and not _naam(s).endswith(_HOUT_BLAD)
                            for s in park.pal], bool)[park.v]
        for x, z in zip(*np.nonzero(rond)):
            col_b = bm[x, :, z]
            tg = top_grond[x, z]
            if gebouwd[x, tg + 1:, z].any():
                continue
            for y in np.nonzero(col_b)[0].tolist():
                if y > tg:
                    park.v[x, y, z] = 0
        gm = _grond_lut(park)[park.v]
        bm = _boom_lut(park)[park.v]
        onder_grond = np.zeros_like(bm)
        onder_grond[:, :-1, :] = gm[:, 1:, :]
        hit2 = (bm & onder_grond).any(axis=1)
        per = {"_voor": int(hit.sum())}
        hit = hit2
    else:
        per = {}
    for x, z in zip(*np.nonzero(hit)):
        k = _zone_naam(park, x, z)
        per[k] = per.get(k, 0) + 1
    return int(hit.sum()), per


def _loop_hoogte(park, x, z, lut_loop):
    """Walking height (y of the surface you stand on, bottom slabs counted half) of a column, or None: the lowest
    solid block at or above the ground (park.hoogte - 2) with two passable blocks above it (so a gate's lintel or a
    lantern post over the path is not taken for the ground)."""
    col = park.v[x, :, z]
    vast = lut_loop[col]
    y0 = max(int(park.hoogte[x, z]) - 2, 1) if getattr(park, "hoogte", None) is not None else 1
    for y in range(y0, H - 2):
        if vast[y] and not vast[y + 1] and not vast[y + 2]:
            st = park.pal[int(col[y])]
            half = "_slab" in st and "type=bottom" in st
            return y + (0.5 if half else 1.0)
    return None


def padsprongen(park, herstel, max_rondes=8):
    """Path columns next to each other whose walking heights differ by more than 1 block (a 2-3 block drop or wall in
    the path). Repaired by raising the lower column with ground + a path top (never cutting: buildings may stand on a
    higher side); repeated, so a slope of at most 1 block per block results."""
    if getattr(park, "pad", None) is None:
        return 0
    kl = np.array([soort(s) for s in park.pal])
    lut = np.isin(kl, ["vast"]) & ~np.array([_naam(s) in ("barrier", "light", "structure_void") for s in park.pal])
    from .terrein import ZONES
    # (the entreedorp lays its own streets and plaza over the old route cells: its paths are its own design)
    m = park.pad & ~park.water
    if getattr(park, "hek", None) is not None:
        m &= ~park.hek          # fence/gate-post columns at the road's edge are not path (you walk past them)
    if getattr(park, "zone_vol", None) is not None:
        m &= park.zone_vol != ZONES.index("entree")
    xs, zs = np.nonzero(m)
    eerste = None
    for ronde in range(max_rondes if herstel else 1):
        if len(lut) < len(park.pal):
            kl = np.array([soort(s) for s in park.pal])
            lut = np.isin(kl, ["vast"])
        hoog = {}
        for x, z in zip(xs.tolist(), zs.tolist()):
            hoog[(x, z)] = _loop_hoogte(park, x, z, lut)
        sprong = []
        for (x, z), h in hoog.items():
            if h is None:
                continue
            for dx, dz in ((1, 0), (0, 1)):
                h2 = hoog.get((x + dx, z + dz))
                if h2 is not None and abs(h2 - h) > 1.01:
                    sprong.append(((x, z), h, (x + dx, z + dz), h2))
        if eerste is None:
            eerste = len(sprong)
        if not herstel or not sprong:
            break
        grond = park.id("minecraft:dirt")
        padb = park.id("minecraft:dirt_path")
        for a, ha, b, hb in sprong:
            laag, hl, hh = (a, ha, hb) if ha < hb else (b, hb, ha)
            hoog_c = b if laag == a else a
            hy = int(np.ceil(hh)) - 1
            top_hoog = _naam(park.pal[int(park.v[hoog_c[0], hy, hoog_c[1]])])
            if not (top_hoog in ("dirt_path", "gravel", "coarse_dirt", "grass_block", "dirt", "packed_ice", "snow_block",
                                 "sand", "red_sand", "mud", "stone_bricks", "cobblestone")
                    or top_hoog.endswith(("_planks", "_slab"))):
                continue                      # the higher side is a structure, not a path: report only
            if getattr(park, "hoogte", None) is not None and hh - (int(park.hoogte[hoog_c]) + 1) > 2.01:
                continue                      # the high side stands on a structure (a mast, a gate): never ramp up to it
            x, z = laag
            doel = int(np.ceil(hh - 1.0))           # new walking height >= hh - 1
            top = int(np.floor(hl)) - 1
            if doel - 1 <= top:
                continue
            for y in range(max(top + 1, 1), doel):
                park.v[x, y, z] = grond
            park.v[x, doel - 1, z] = padb
            for y in range(doel, min(doel + 3, H)):
                if soort(park.pal[int(park.v[x, y, z])]) in ("plant", "lucht", "staand", "blad", "rank"):
                    park.v[x, y, z] = 0
            if getattr(park, "hoogte", None) is not None:
                park.hoogte[x, z] = max(int(park.hoogte[x, z]), doel - 1)
    return eerste or 0


def void(park, herstel):
    """Columns near the park (the park + 40 blocks around it, everything you can see from walkable ground) that are
    empty or reach no higher than y 20: you would look into the void (or fall). Filled up to the lowest neighbour."""
    if getattr(park, "binnen", None) is None:
        return 0
    from scipy.ndimage import binary_dilation as _dil
    gebied = _dil(park.binnen, iterations=40)
    vast = park.v != 0
    top = _bovenste(vast)
    kuil = gebied & (top < 20)
    n = int(kuil.sum())
    if herstel and n:
        stone = park.id("minecraft:stone")
        h = getattr(park, "hoogte", None)
        for x, z in zip(*np.nonzero(kuil)):
            buren = [int(top[x2, z2]) for x2, z2 in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1))
                     if 0 <= x2 < W and 0 <= z2 < W and top[x2, z2] >= 20]
            doel = min(buren) if buren else (int(h[x, z]) if h is not None else WATER_Y)
            for y in range(1, max(doel, 2)):
                if park.v[x, y, z] == 0:
                    park.v[x, y, z] = stone
    return n


# vehicle clearance boxes: (half width, blocks below the path point, blocks above it)
_RIT_DOOS = {"route": (1.6, 0, 3), "kano": (0.6, 0, 2), "boot": (1.2, 0, 2), "kabelbaan": (1.2, 0, 5)}
# (half widths in blocks = the vehicle's real footprint around its path point: the truck TRUCK_HALF 1.6 (zone_s3_rit), the
#  gondola and the boat ~1.2, the canoe 0.6; a block counts when its square comes closer to the point than that)
# (kabelbaan: its points are the gondola FLOOR (zone_s3_rit: gondel_y = dek + 1, the cable at dek + 6), so the box is the
#  gondola body from the floor up to just under the cable; the station deck it stands on is under the box)


def _rit_punten(park, naam):
    s3 = getattr(park, "_s3", None) or {}
    d = s3.get(naam)
    if not d:
        return []
    if isinstance(d, dict) and d.get("punten"):
        return [tuple(p[:3]) for p in d["punten"]]
    if isinstance(d, dict) and d.get("routes"):
        uit = []
        for r in d["routes"]:                         # the glass-bottom boats (one route per zone)
            uit += [tuple(p[:3]) for p in r.get("punten", [])]
        return uit
    if isinstance(d, dict) and d.get("lus"):
        return [tuple(p[:3]) for p in d["lus"]]       # the kabelbaan loop (2 lines + turning loops): where gondolas ride
    if isinstance(d, dict) and d.get("a") and d.get("b"):
        a, b = d["a"], d["b"]
        n = max(2, int(np.hypot(b[0] - a[0], b[2] - a[2])))
        return [tuple(a[k] + (b[k] - a[k]) * i / n for k in range(3)) for i in range(n + 1)]
    if isinstance(d, dict) and d.get("lussen"):
        uit = []
        for lus in d["lussen"]:
            uit += [tuple(p[:3]) for p in lus.get("punten", [])]
        return uit
    return []


def doorgang(park, herstel):
    """Free passage along the rides (park._s3: route = truck, kano, boot, kabelbaan): every sample point's clearance box
    must be free of solid blocks (gates and the ride's own path/deck under it are fine). Leaves and plants in a box are
    removed; the rest is reported per ride (x, y, z, block)."""
    kl = np.array([soort(s) for s in park.pal])
    hard = np.isin(kl, ["vast", "hek", "muur", "ruit", "deur"])
    zacht = np.isin(kl, ["blad", "plant", "rank", "staand", "lelie"])
    totaal, rapport = 0, {}
    for naam, (hb, onder, boven) in _RIT_DOOS.items():
        pts = _rit_punten(park, naam)
        geblokt = []
        for (px, py, pz) in pts:
            # the ride's own ground under the point is not in the box; a truck climbs a 1-block bump (smoothed points)
            y0 = int(np.ceil(py - 0.01)) - onder + (1 if naam == "route" else 0)
            y1 = int(np.ceil(py - 0.01)) + boven - 1
            hit = None
            for x in range(int(np.floor(px - hb)), int(np.floor(px + hb)) + 1):
                for z in range(int(np.floor(pz - hb)), int(np.floor(pz + hb)) + 1):
                    # distance from the point to the block's square (0 when the point is inside it)
                    ddx = max(x - px, 0.0, px - (x + 1))
                    ddz = max(z - pz, 0.0, pz - (z + 1))
                    if ddx * ddx + ddz * ddz >= hb * hb:
                        continue
                    for y in range(max(y0, 0), min(y1, H - 1) + 1):
                        if not (0 <= x < W and 0 <= z < W):
                            continue
                        if naam == "route" and getattr(park, "pad", None) is not None and not park.pad[x, z]:
                            continue                   # the truck drives on the path: only the path cells count
                        i = int(park.v[x, y, z])
                        if i >= len(hard):
                            kl = np.array([soort(s) for s in park.pal])
                            hard = np.isin(kl, ["vast", "hek", "muur", "ruit", "deur"])
                            zacht = np.isin(kl, ["blad", "plant", "rank", "staand", "lelie"])
                        if zacht[i] and herstel:
                            park.v[x, y, z] = 0
                        elif hard[i]:
                            hit = (x, y, z, _naam(park.pal[i]))
            if hit:
                geblokt.append(hit)
        totaal += len(geblokt)
        if geblokt:
            rapport[naam] = geblokt
    return totaal, rapport


# ======================================================================================================================
# round 4: reachability (bereik) and narrow pits (kuil)
_KLIM = ("ladder", "vine", "scaffolding", "twisting_vines", "weeping_vines", "cave_vines")
_DOORLOOP = ("glow_lichen", "sculk_vein", "cobweb", "structure_void", "_trapdoor", "snow")
_HALF = ("stoel", "_bed")


@lru_cache(maxsize=None)
def _loop_klasse(s):
    """For walking: 'P' passable (air, plants, water, doors, gates, thin things), 'F' full floor, 'H' half floor (bottom
    slab, stairs, a chair, a bed), 'C' climbable (ladder, vines; also passable), 'W' water (passable, swim), 'X' solid
    but not standable (fences, walls, panes: you cannot step onto them)."""
    n = _naam(s)
    k = soort(s)
    props = split_state(s)[1]
    if n in ("water", "bubble_column") or k == "water":
        return "W"
    if any(n == w or n.endswith(w) for w in _KLIM):
        return "C"
    if n in ("light", "snow") or any(w in n for w in _DOORLOOP if w not in ("snow",)):
        return "P"
    if k in ("lucht", "deur", "poort", "grote_poort", "rank", "wand", "lelie", "hangend"):
        return "P"
    if k == "plant":
        return "X" if n == "bamboo" else "P"
    if k == "staand":
        dun = ("carpet", "torch", "sign", "banner", "pressure_plate", "rail", "sea_pickle", "coral", "redstone_wire",
               "candle")
        return "P" if any(w in n for w in dun) else "H"
    if k in ("hek", "muur", "ruit"):
        return "X"
    if n.endswith("_slab"):
        return "H" if props.get("type", "bottom") == "bottom" else "F"
    if n.endswith("_stairs") or any(w in n for w in _HALF):
        return "H"
    return "F"


def _loop_volume(park, y0):
    """uint8 bit volume over [x, y - y0, z]: 1 passable, 2 full floor, 4 half floor, 8 climb, 16 water."""
    bits = {"P": 1, "F": 2, "H": 4, "C": 8 | 1, "W": 16 | 1, "X": 0}
    lut = np.array([bits[_loop_klasse(s)] for s in park.pal], np.uint8)
    return lut[park.v[:, y0:, :]]


def _knopen(park, y0=20):
    """Walk nodes: dict col -> sorted feet heights f (half blocks: f = 2*y feet in cell y, 2*y+1 standing on a half
    block in cell y) and dict (col, f) -> 'l' land / 's' swim or climb."""
    V = _loop_volume(park, y0)
    P = (V & 1) > 0
    F = (V & 2) > 0
    Hf = (V & 4) > 0
    C = (V & 8) > 0
    Wt = (V & 16) > 0
    n = V.shape[1]
    knopen = {}
    soorten = {}

    def voeg(m, f_van, kind):
        xs, ys, zs = np.nonzero(m)
        cols = (xs.astype(np.int64) * W + zs).tolist()
        fs = f_van(ys + y0).tolist()
        for col, f in zip(cols, fs):
            knopen.setdefault(col, set()).add(f)
            if soorten.get((col, f)) != "l":
                soorten[(col, f)] = kind

    m = np.zeros_like(P)
    m[:, 1:n - 1, :] = F[:, :n - 2, :] & P[:, 1:n - 1, :] & P[:, 2:, :]          # on a full block (also wading)
    voeg(m, lambda y: 2 * y, "l")
    m = np.zeros_like(P)
    m[:, :n - 2, :] = Hf[:, :n - 2, :] & P[:, 1:n - 1, :] & P[:, 2:, :]           # on a half block in cell y
    voeg(m, lambda y: 2 * y + 1, "l")
    voeg(Wt | C, lambda y: 2 * y, "s")                                           # swimming / climbing
    return {c: sorted(v) for c, v in knopen.items()}, soorten, P, Hf


def _past(P, Hf, y0, x, z, h2):
    """Does a player body (1.8 high) fit in column x,z with the feet at h2/2 (half blocks)? A half block in the cell
    under the feet is fine (the feet stand on top of it)."""
    h = h2 / 2.0
    c = int(h)
    while c < h + 1.8:
        i = c - y0
        if i < 0 or i >= P.shape[1]:
            return False
        if not P[x, i, z] and not (Hf[x, i, z] and h >= c + 0.5):
            return False
        c += 1
    return True


def _start(park, knopen, soorten):
    ix, iy, iz = int(park.ingang[0]), int(park.ingang[1]), int(park.ingang[2])
    for r in range(0, 8):
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                col = (ix + dx) * W + iz + dz
                for f in knopen.get(col, []):
                    if abs(f - 2 * iy) <= 4 and soorten[(col, f)] == "l":
                        return (col, f)
    return None


def bereik(park, y0=20):
    """Flood fill from the entrance square over everything a player can walk: step up <= 1 block, drop <= 3, a body 2
    high, doors and gates open, ladders, swimming. Returns the reached set of (col, f)."""
    from collections import deque
    knopen, soorten, P, Hf = _knopen(park, y0)
    park._bereik_P = (P, y0)
    start = _start(park, knopen, soorten)
    gezien = set()
    if start is None:
        return gezien
    q = deque([start])
    gezien.add(start)
    while q:
        col, f = q.popleft()
        x, z = divmod(col, W)
        kind = soorten[(col, f)]
        for g in knopen[col]:                       # inside the column: swim / climb up and down (and out of it)
            if g != f and abs(g - f) <= 2 and (kind == "s" or soorten[(col, g)] == "s") and (col, g) not in gezien:
                gezien.add((col, g))
                q.append((col, g))
        for ncol in (col + W, col - W, col + 1, col - 1):
            if ncol < 0 or ncol >= W * W or (abs(ncol - col) == 1 and ncol // W != x):
                continue
            nx, nz = divmod(ncol, W)
            for g in knopen.get(ncol, ()):
                d = g - f
                if not (-6 <= d <= 2) or (ncol, g) in gezien:
                    continue
                # the body at the higher of the two heights must fit in both columns (no head through a lintel when
                # stepping down off a stair, no jumping up under a low ceiling)
                hoog = max(f, g)
                # (also swimming / climbing: no swimming up through a glass wall onto the floor behind it)
                if not (_past(P, Hf, y0, nx, nz, hoog) and _past(P, Hf, y0, x, z, hoog)):
                    continue
                gezien.add((ncol, g))
                q.append((ncol, g))
    return gezien


def _doelen(park):
    """Targets a player must be able to walk to: (naam, x, y_feet, z, horizontal radius, tolerance in half blocks)."""
    uit = []
    deur_lut = np.array([("_door" in _naam(s) and "trapdoor" not in _naam(s)
                          and split_state(s)[1].get("half") == "lower") for s in park.pal], bool)
    xs, ys, zs = np.nonzero(deur_lut[park.v])
    for x, y, z in zip(xs.tolist(), ys.tolist(), zs.tolist()):
        uit.append(("deur", x, y, z, 1, 2))
    for st in (getattr(park, "stoelen", None) or []):
        uit.append(("stoel", int(st[0]), int(st[1]), int(st[2]), 1, 3))
    for k in (getattr(park, "kijkplekken", None) or []):
        if k.get("stoel") is None and k["naam"].startswith("glijbaan"):
            continue                                # the penguins' slide points, not a place for people
        ox, oy, oz = k["oog"]
        uit.append((f"kijkplek {k['naam']}", int(ox), int(round(oy - 1.6)), int(oz), 2, 3))
    for g in (getattr(park, "geheimpjes", None) or []):
        uit.append(("geheimpje", int(g[0]), int(g[1]), int(g[2]), 2, 6))   # (a secret may sit up to 3 above the feet)
    for st in (getattr(park, "steigers", None) or []):
        uit.append((f"steiger {st['naam']}", int(st["pos"][0]), int(st["pos"][1]), int(st["pos"][2]), 2, 3))
    return uit


def bereik_rapport(park):
    """(count, list) of targets nobody can walk to: (naam, x, y, z, zone)."""
    gezien = bereik(park)
    if not gezien:
        return 1, [("start niet gevonden",) + tuple(int(c) for c in park.ingang[:3]) + ("?",)]
    bereikt = {}
    for col, f in gezien:
        bereikt.setdefault(col, []).append(f)
    mis = []
    P, y0 = park._bereik_P

    def open_lijn(cx, cz, f, y):
        """(Round 5) Nothing solid between the stand height and the target in that column: standing on the roof right
        above a target (the snow dome over the ice cave's skull) is not reaching it."""
        yf = f // 2
        lo, hi = (y + 1, yf - 1) if yf > y else (yf + 2, y - 1)
        return all(P[cx, c - y0, cz] for c in range(lo, hi + 1) if 0 <= c - y0 < P.shape[1])

    for (naam, x, y, z, r, tol) in _doelen(park):
        if not (0 <= x < W and 0 <= z < W):
            continue
        if getattr(park, "binnen", None) is not None and not park.binnen[x, z]:
            continue
        ok = any(abs(f - 2 * y) <= tol and open_lijn(x + dx, z + dz, f, y)
                 for dx in range(-r, r + 1) for dz in range(-r, r + 1)
                 for f in bereikt.get((x + dx) * W + z + dz, ()))
        if not ok:
            mis.append((naam, x, y, z, _zone_naam(park, x, z)))
    park._bereik = gezien
    return len(mis), mis


def kuilen(park, herstel=False):
    """Narrow pits in walkable ground: a dry 1x1 column whose ground lies >= 2 blocks below all four neighbours, open
    to the sky (you fall in, nothing to see: the 1-wide hole at the top of the glijbaan). Repaired by filling it up to
    the lowest neighbour with that neighbour's blocks."""
    if getattr(park, "hoogte", None) is None:
        return 0
    hoog = park.hoogte
    binnen = park.binnen if getattr(park, "binnen", None) is not None else np.ones((W, W), bool)
    hn = np.full((W, W), -1, int)
    hn[1:-1, 1:-1] = np.minimum.reduce([hoog[2:, 1:-1], hoog[:-2, 1:-1], hoog[1:-1, 2:], hoog[1:-1, :-2]])
    nat = park.water if getattr(park, "water", None) is not None else np.zeros((W, W), bool)
    n = 0
    for x, z in np.argwhere((hn - hoog >= 2) & binnen & ~nat).tolist():
        top = int(hoog[x, z])
        if any(park.v[x, y, z] and soort(park.pal[park.v[x, y, z]]) not in ("plant", "lucht")
               for y in range(top + 1, min(int(hn[x, z]) + 3, H))):
            continue
        n += 1
        if herstel:
            buur = min(((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)), key=lambda c: hoog[c])
            blok = park.v[buur[0], int(hoog[buur]), buur[1]]
            onder = park.v[buur[0], max(int(hoog[buur]) - 1, 0), buur[1]] or blok
            for y in range(top + 1, int(hn[x, z]) + 1):
                park.v[x, y, z] = blok if y == int(hn[x, z]) else onder
            hoog[x, z] = hn[x, z]
    return n


# ======================================================================================================================
def rapport(park):
    return _loop(park, herstel=False)


def herstel(park, log=print):
    voor = _loop(park, herstel=True, log=log)
    na = _loop(park, herstel=False)
    log("  controle (voor -> na): " + ", ".join(f"{k} {voor[k]}->{na[k]}" for k in voor if not k.startswith("_")))
    if na["_platen"]:
        log("  platen (x, z, kolommen, blok, y, zone): " + "; ".join(str(p) for p in na["_platen"][:20]))
    if na["_ingegraven"]:
        log(f"  ingegraven bomen per zone: {na['_ingegraven']}")
    for rit, lijst in na["_doorgang"].items():
        log(f"  {rit} geblokt ({len(lijst)} punten), eerste: {lijst[:8]}")
    if na["_zweef"]:
        log(f"  zwevende stukken (niet weggehaald, x y z blokken): {na['_zweef'][:12]}")
    if na["_kano"]:
        log("  kano kan niet onder door (x, z, kolommen, zone): " + "; ".join(str(p) for p in na["_kano"]))
    if na["_bereik"]:
        log(f"  onbereikbaar ({len(na['_bereik'])}): " + "; ".join(str(p) for p in na["_bereik"][:80]))
    return voor, na


def _loop(park, herstel, log=None):
    uit = {}
    uit["blad"] = blad(park, herstel)
    # round 3: the shape repairs first (they add / remove blocks the later checks look at)
    n, per = ingegraven(park, herstel)
    uit["ingegraven"] = per.pop("_voor", n) if herstel else n
    uit["_ingegraven"] = per
    uit["padsprong"] = padsprongen(park, herstel)
    if herstel and getattr(park, "_s3", None) and callable(getattr(park, "_s3_ververs", None)):
        park._s3_ververs(park)          # the truck follows the road as repaired (through the gates' middle); it may
                                        # clear tree parts in its footprint: the later checks (steun, zweef) tidy up
    uit["void"] = void(park, herstel)
    uit["hekgat"] = hekgaten(park, herstel)
    uit["steun"] = steun(park, herstel)
    if herstel:
        for _ in range(4):                  # removing a base can leave the next block unsupported (cane, doors...)
            if not steun(park, True):
                break
    uit["lek"] = lekken(park, herstel)
    uit["val"] = vallen(park, herstel)
    uit["gat"] = gaten(park, herstel)
    # repairs may have put blocks next to fences (bank blocks): connections last
    uit["verbinding"] = verbindingen(park, herstel)
    z, rap = zweven(park, herstel)
    uit["zweef"] = z
    uit["_zweef"] = rap
    k = kano(park)
    uit["kano"] = len(k)
    uit["_kano"] = k
    n, lijst = platen(park)
    uit["plaat"] = n
    uit["_platen"] = lijst
    n, rap = doorgang(park, herstel)
    uit["doorgang"] = n
    uit["_doorgang"] = rap
    uit["kuil"] = kuilen(park, herstel)
    if not herstel:                          # (round 4) reachability: only counted on the final (report) pass
        n, mis = bereik_rapport(park)
        uit["bereik"] = n
        uit["_bereik"] = mis
    else:
        uit["bereik"] = 0
        uit["_bereik"] = []
    return uit
