"""
data/guhs/safari/plekken.json: where every species lives in the park (the game only reads this).

Counts from the design species sheet (ontwerp-safaripark-en-bouwen.md 1.6, ronde 12: every animal at least 2) plus the
9 extra animals (ronde 38-39). A species is split into homes of at most 4 (2 adults + up to 2 young), so herds get
several homes close together. The Gouden dikdik does not live in the park.
"""
import math

import numpy as np
from scipy.ndimage import distance_transform_edt

from . import kit
from .terrein import ZONES

# soort: (zone, total in park, kind of spot, home radius)
#   kind: land | open | boom | water | diep | verstop:<reservering> | plek:<reservering> | eiland | grot:<reservering>
SOORTEN = {
    "leeuw": ("savanne", 4, "plek:trotse_rots", 14), "luipaard": ("savanne", 2, "boom", 10),
    "olifant": ("savanne", 4, "plek:vadse_baobab_poel", 18), "neushoorn": ("savanne", 3, "open", 16),
    "kafferbuffel": ("savanne", 6, "open", 18), "dikdik": ("savanne", 2, "verstop", 7),
    "giraffe": ("savanne", 4, "boom", 18), "zebra": ("savanne", 8, "plek:trekvlakte", 20),
    "stokstaartje": ("savanne", 10, "plek:termietenheuvels", 9), "jachtluipaard": ("savanne", 2, "open", 14),
    "struisvogel": ("savanne", 4, "open", 18),
    "zeekoe": ("delta", 2, "diep", 12), "nijlpaard": ("delta", 3, "plek:nijlpaardenpoel", 10),
    "flamingo": ("delta", 8, "plek:flamingomeer", 10), "capibara": ("delta", 4, "oever", 10),
    "krokodil": ("delta", 2, "plek:krokodillenbank", 8),
    "rode_panda": ("bamboe", 3, "boom", 10), "reuzenpanda": ("bamboe", 2, "plek:bamboedoolhof", 10),
    "gibbon": ("bamboe", 3, "boom", 12),
    "luiaard": ("regenwoud", 2, "boom", 6), "okapi": ("regenwoud", 2, "verstop:okapi_dal", 7),
    "orang_oetan": ("regenwoud", 3, "plek:reuzenboom", 12), "toekan": ("regenwoud", 4, "boom", 14),
    "gekko": ("regenwoud", 4, "boom", 8), "glimworm": ("regenwoud", 6, "grot:waterval_grot", 5),
    "pinguin": ("ijskust", 8, "plek:pinguinglijbaan", 12), "ijsbeer": ("ijskust", 2, "plek:ijsbergkliffen", 14),
    "zeehond": ("ijskust", 4, "oever", 10), "narwal": ("ijskust", 2, "diep", 14), "octopus": ("ijskust", 2, "diep", 8),
    "sabeltandtijger": ("oertijd", 2, "verstop:ijsgrot_fossiel", 7), "mammoet": ("oertijd", 3, "open", 18),
    "wolharige_neushoorn": ("oertijd", 2, "open", 16),
    "bever": ("europa", 2, "verstop:beverdam", 6), "vuursalamander": ("europa", 2, "verstop", 5),
    "egel": ("europa", 6, "land", 8), "otter": ("europa", 3, "oever", 10), "das": ("europa", 2, "verstop:dassenheuvel", 6),
    "edelhert": ("europa", 4, "open", 16), "ooievaar": ("europa", 3, "oever", 12), "specht": ("europa", 2, "boom", 8),
    "koala": ("australie", 2, "plek:eucalyptusbos", 8), "kangoeroe": ("australie", 4, "open", 18),
    "wombat": ("australie", 2, "plek:wombatheuvel", 7), "quokka": ("australie", 2, "eiland", 4),
    "prieelvogel": ("australie", 2, "boom", 8),
    # the aaihoek in the entreedorp (the lobby slice builds the pens; 1 adult + 1 young each)
    "geitje": ("entree", 2, "aaihoek", 4), "alpaca": ("entree", 2, "aaihoek", 4),
    "cavia": ("entree", 2, "aaihoek", 3), "ezeltje": ("entree", 2, "aaihoek", 4),
}
WATERDIER = {"zeekoe", "narwal", "octopus"}
HALFWATER = {"nijlpaard", "krokodil", "flamingo", "capibara", "zeehond", "otter", "bever", "pinguin", "ooievaar", "ijsbeer"}
BOOMDIER = {"luipaard", "rode_panda", "gibbon", "luiaard", "toekan", "gekko", "koala", "specht", "prieelvogel"}


def _huizen(n, aaihoek=False):
    """Split a total into homes of at most 4: (adults, young)."""
    if aaihoek:
        return [(1, 1)] * (n // 2)
    uit = []
    while n > 0:
        k = min(4, n)
        if n - k == 1:          # never leave a lonely 1 for the last home
            k = 3
        v = 2 if k >= 2 else 1
        uit.append((v, k - v))
        n -= k
    return uit


def maak(park):
    rng = np.random.default_rng(kit.SEED + 9)
    pad_d = distance_transform_edt(~park.pad)
    land = park.binnen & ~park.water & (park.hek_afstand > 4)
    diep_water = park.water & (park.hoogte <= kit.WATER_Y - 4) & park.binnen
    water_d = distance_transform_edt(~park.water)
    gebruikt = []                 # (x, z) of homes already given (spread them out)
    bomen = np.array([(x, z) for x, _, z, _ in park.bomen]) if park.bomen else np.zeros((0, 2))

    def kies(mask, rond=None, r=None, afstand=10):
        xs, zs = np.nonzero(mask)
        if rond is not None:
            d = np.hypot(xs - rond[0], zs - rond[1])
            sel = d <= r
            if sel.any():
                xs, zs = xs[sel], zs[sel]
        if not len(xs):
            return None
        best = None
        for _ in range(60):
            i = int(rng.integers(0, len(xs)))
            x, z = int(xs[i]), int(zs[i])
            ver = min((math.hypot(x - a, z - b) for a, b in gebruikt), default=999)
            if ver >= afstand:
                return x, z
            if best is None or ver > best[0]:
                best = (ver, x, z)
        return best[1], best[2]

    plekken = []
    for soort, (zone, n, soortplek, straal) in SOORTEN.items():
        zi = ZONES.index(zone)
        inzone = park.zone == zi
        huizen = _huizen(n, soortplek == "aaihoek")
        anker = None
        if ":" in soortplek:
            anker = kit.RESERVERINGEN[soortplek.split(":")[1]][:2]
        for (v, j) in huizen:
            if soort in WATERDIER:
                m = inzone & (diep_water if diep_water[inzone].any() else park.water)
                plek = kies(m, None, None, 14)
            elif soortplek == "oever":
                plek = kies(inzone & land & (water_d <= 4) & (water_d >= 1), anker, 40)
            elif soortplek == "eiland":
                qx, qz = kit.PARK_OFFSET + 215, kit.PARK_OFFSET + 300
                plek = (qx, qz)
            elif soortplek == "aaihoek":
                px, _, pz = park.plein
                plek = kies((park.zone == zi) & land & (pad_d > 2), (px + 26, pz), 16, 5)
            elif soortplek == "boom" and len(bomen):
                in_z = [(x, z) for x, z in bomen if park.zone[x, z] == zi]
                if in_z:
                    x, z = in_z[int(rng.integers(0, len(in_z)))]
                    plek = (int(x) + 1, int(z) + 1)
                else:
                    plek = kies(inzone & land)
            elif anker is not None:
                plek = kies(inzone & land, anker, 16, 6) or kies(inzone & land)
            elif soortplek == "verstop":
                plek = kies(inzone & land & (pad_d > 25), None, None, 20) or kies(inzone & land)
            elif soortplek == "open":
                plek = kies(inzone & land & (pad_d > 6), None, None, 22) or kies(inzone & land)
            else:
                plek = kies(inzone & land, None, None, 14)
            if plek is None:
                raise SystemExit(f"geen plek voor {soort} in {zone}")
            x, z = plek
            if soort in WATERDIER:
                y = int(park.hoogte[x, z]) + 1
            else:
                y = int(park.hoogte[x, z]) + 1
                # stand on top of whatever is there (rocks)
                while y < kit.H - 2 and park.v[x, y, z] != 0 and not park.get(x, y, z).startswith(
                        ("minecraft:short_grass", "minecraft:tall_grass", "minecraft:fern", "minecraft:large_fern",
                         "minecraft:snow", "minecraft:dead_bush", "minecraft:water")):
                    y += 1
            gebruikt.append((x, z))
            plekken.append({"soort": soort, "zone": zone, "centrum": [int(x), int(y), int(z)], "straal": straal,
                            "volwassenen": v, "jongen": j,
                            "water": soort in WATERDIER or soort in HALFWATER, "boom": soort in BOOMDIER})
    return plekken


# --- test round 2: more animals, spread over their zone -------------------------------------------------------------
# rare / hard-to-find species and the aaihoek keep their numbers; every other home gets EXTRA_HUIZEN clones elsewhere in
# its zone (each its own home with its own wandering range), so the park has ~3x the animals.
ZELDZAAM = {"narwal", "okapi", "sabeltandtijger", "quokka", "vuursalamander", "das", "glimworm", "bever", "gouden_dikdik"}
EXTRA_HUIZEN = 2
PLANTEN = ("minecraft:short_grass", "minecraft:tall_grass", "minecraft:fern", "minecraft:large_fern", "minecraft:snow",
           "minecraft:dead_bush", "minecraft:short_dry_grass", "minecraft:tall_dry_grass", "minecraft:bush",
           "minecraft:firefly_bush", "minecraft:leaf_litter", "minecraft:pink_petals", "minecraft:wildflowers")


def _staan_y(park, x, z):
    """y to stand on in this column: on top of the ground and of anything solid on it (rocks), not inside plants."""
    y = int(park.hoogte[x, z]) + 1
    while y < kit.H - 2 and park.v[x, y, z] != 0 and not park.get(x, y, z).startswith(PLANTEN + ("minecraft:water",)):
        y += 1
    return y


def _natuurlijk(park, x, z):
    """Nothing built or grown over this column (no building, tree crown or landmark within 4 above the ground)."""
    h = int(park.hoogte[x, z])
    for y in range(h + 1, min(h + 5, kit.H)):
        if park.v[x, y, z] != 0 and not park.get(x, y, z).startswith(PLANTEN):
            return False
    return True


def cel_zone(park, cel=16):
    """Per column the zone the GAME sees there: the majority zone of its 16x16 cell, exactly like zones.json
    (safari_park.zones_json: '.' = -1 when less than a quarter of the cell is park). A home must lie in a column where
    this equals its own zone, else the game keeps the animals in another zone than the generator meant."""
    n = kit.W // cel
    uit = np.full((kit.W, kit.W), -1, dtype=np.int16)
    for cz in range(n):
        for cx in range(n):
            blok = park.zone[cx * cel:(cx + 1) * cel, cz * cel:(cz + 1) * cel]
            binnen = blok[blok >= 0]
            if binnen.size >= cel * cel * 0.25:
                uit[cx * cel:(cx + 1) * cel, cz * cel:(cz + 1) * cel] = int(np.bincount(binnen).argmax())
    return uit


def vermenigvuldig(park, plekken, log=print):
    """After the zone hooks: land animals never live with their centre in water, luiaards hang near a path where people
    can see them, and every normal home gets EXTRA_HUIZEN clones spread over the zone (right terrain per species)."""
    rng = np.random.default_rng(kit.SEED + 31)
    pad_d = distance_transform_edt(~park.pad)
    water_d = distance_transform_edt(~park.water)
    land = park.binnen & ~park.water & (park.hek_afstand > 4) & ~park.pad
    diep_water = park.water & (park.hoogte <= kit.WATER_Y - 3) & park.binnen
    bomen = [(int(x), int(z)) for x, _, z, _ in park.bomen]
    cz = cel_zone(park)

    def inzone(zi):
        return (park.zone == zi) & (cz == zi)

    def soortplek(soort):
        return SOORTEN.get(soort, (None, 0, "land", 10))[2]

    def masker(soort, zi):
        inzone_ = inzone(zi)
        if soort in WATERDIER:
            m = inzone_ & diep_water
            return m if m.any() else inzone_ & park.water
        if soort in HALFWATER:
            return inzone_ & land & (water_d >= 1) & (water_d <= 5)
        if soortplek(soort) == "open":
            return inzone_ & land & (pad_d > 5)
        return inzone_ & land

    def boom_plekken(zi, bij_pad=None):
        uit = [(x, z) for x, z in bomen if park.zone[x, z] == zi and park.binnen[x, z]
               and 0 <= x + 1 < kit.W and cz[x + 1, z] == zi]          # the home sits at x + 1
        if bij_pad is not None:
            dicht = [(x, z) for x, z in uit if pad_d[x, z] <= bij_pad]
            uit = dicht or uit
        return uit

    def kies(kandidaten, eigen, alle, min_eigen):
        """The candidate farthest from the species' own homes (at least min_eigen if possible), not on top of others."""
        best, best_d = None, -1.0
        for x, z in kandidaten:
            d_eigen = min((math.hypot(x - a, z - b) for a, b in eigen), default=999.0)
            d_alle = min((math.hypot(x - a, z - b) for a, b in alle), default=999.0)
            if d_alle < 8:
                continue
            score = min(d_eigen, min_eigen * 2.5)
            if score > best_d:
                best, best_d = (x, z), score
        return best

    def steekproef(mask, n=160):
        xs, zs = np.nonzero(mask)
        if not len(xs):
            return []
        idx = rng.choice(len(xs), size=min(n, len(xs)), replace=False)
        return [(int(xs[i]), int(zs[i])) for i in idx]

    # 1. land animals never live in the water (the olifant walked under water in the savanne pond)
    for p in plekken:
        s = p["soort"]
        x, _, z = p["centrum"]
        if s in WATERDIER or s in HALFWATER or not (0 <= x < kit.W and 0 <= z < kit.W) or not park.water[x, z]:
            continue
        zi = ZONES.index(p["zone"])
        m = inzone(zi) & land
        xs, zs = np.nonzero(m)
        if len(xs):
            i = int(np.argmin(np.hypot(xs - x, zs - z)))
            nx, nz = int(xs[i]), int(zs[i])
            log(f"    {s}: thuis uit het water ({x},{z}) -> ({nx},{nz})")
            p["centrum"] = [nx, _staan_y(park, nx, nz), nz]

    # 2. luiaards hang in trees next to a path (visible from the ground / the boomkronenpad)
    for p in plekken:
        if p["soort"] != "luiaard":
            continue
        x, _, z = p["centrum"]
        if pad_d[x, z] <= 6:
            continue
        zi = ZONES.index(p["zone"])
        bij = boom_plekken(zi, bij_pad=6)
        if bij:
            bx, bz = min(bij, key=lambda b: math.hypot(b[0] - x, b[1] - z))
            p["centrum"] = [bx + 1, _staan_y(park, bx + 1, bz), bz]
            p["straal"] = min(p["straal"], 5)

    # 3. clones, spread over the zone
    alle = [(p["centrum"][0], p["centrum"][2]) for p in plekken]
    extra = []
    for p in list(plekken):
        s = p["soort"]
        if s in ZELDZAAM or p["zone"] == "entree":
            continue
        zi = ZONES.index(p["zone"])
        eigen = [(q["centrum"][0], q["centrum"][2]) for q in plekken + extra if q["soort"] == s]
        min_eigen = max(24, 2 * p["straal"])
        for _ in range(EXTRA_HUIZEN):
            if p.get("boom") or soortplek(s) == "boom":
                kand = boom_plekken(zi, bij_pad=6 if s == "luiaard" else None)
                kand = [kand[i] for i in rng.permutation(len(kand))[:200]] if kand else []
                kand = [(x + 1, z) for x, z in kand if 0 <= x + 1 < kit.W]
            else:
                kand = [(x, z) for x, z in steekproef(masker(s, zi), 240)
                        if s in WATERDIER or _natuurlijk(park, x, z)]
            plek = kies(kand, eigen, alle, min_eigen)
            if plek is None:
                log(f"    {s}: geen extra plek in {p['zone']}")
                break
            x, z = plek
            y = int(park.hoogte[x, z]) + 1 if s in WATERDIER else _staan_y(park, x, z)
            q = dict(p, centrum=[x, y, z])
            extra.append(q)
            eigen.append((x, z))
            alle.append((x, z))
    plekken.extend(extra)

    # 4. every home (zone hooks and clones) where the game sees its own zone (zones.json cell), else the nearest such spot
    #    of the right terrain (round 2 log: a zeekoe home in regenwoud, an octopus home in europa, a toekan home in delta)
    for p in plekken:
        x, _, z = p["centrum"]
        zi = ZONES.index(p["zone"])
        if 0 <= x < kit.W and 0 <= z < kit.W and cz[x, z] == zi:
            continue
        s = p["soort"]
        if p.get("boom") or soortplek(s) == "boom":
            kand = [(bx + 1, bz) for bx, bz in boom_plekken(zi)]
        else:
            xs, zs = np.nonzero(masker(s, zi))
            kand = list(zip(xs.tolist(), zs.tolist()))
        if not kand:
            xs, zs = np.nonzero(inzone(zi) & park.binnen)
            kand = list(zip(xs.tolist(), zs.tolist()))
        nx, nz = min(kand, key=lambda c: (c[0] - x) ** 2 + (c[1] - z) ** 2)
        ny = int(park.hoogte[nx, nz]) + 1 if s in WATERDIER else _staan_y(park, nx, nz)
        log(f"    {s}: thuis lag in een andere zone ({x},{z}) -> ({nx},{nz}) in {p['zone']}")
        p["centrum"] = [nx, ny, nz]
    return plekken


def controleer(plekken):
    tel = {}
    for p in plekken:
        tel[p["soort"]] = tel.get(p["soort"], 0) + p["volwassenen"] + p["jongen"]
    for soort, (_, n, _, _) in SOORTEN.items():
        if soort in ZELDZAAM or SOORTEN[soort][0] == "entree":
            assert tel.get(soort) == n, (soort, tel.get(soort), n)
        else:
            assert tel.get(soort, 0) >= n, (soort, tel.get(soort), n)     # at least the sheet's number (clones on top)
    assert "gouden_dikdik" not in tel
    return sum(tel.values())
