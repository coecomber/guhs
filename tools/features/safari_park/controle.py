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
    if n.endswith("_fence_gate"):
        return "poort"
    if n.endswith("_fence"):
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
    hekachtig = np.isin(kl, ["hek", "ruit", "muur", "poort"])
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


# ======================================================================================================================
def rapport(park):
    return _loop(park, herstel=False)


def herstel(park, log=print):
    voor = _loop(park, herstel=True, log=log)
    na = _loop(park, herstel=False)
    log("  controle (voor -> na): " + ", ".join(f"{k} {voor[k]}->{na[k]}" for k in voor if k not in ("_kano", "_zweef")))
    if na["_zweef"]:
        log(f"  zwevende stukken (niet weggehaald, x y z blokken): {na['_zweef'][:12]}")
    if na["_kano"]:
        log("  kano kan niet onder door (x, z, kolommen, zone): " + "; ".join(str(p) for p in na["_kano"]))
    return voor, na


def _loop(park, herstel, log=None):
    uit = {}
    uit["blad"] = blad(park, herstel)
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
    return uit
