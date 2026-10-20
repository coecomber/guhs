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
HALFWATER = {"nijlpaard", "krokodil", "flamingo", "capibara", "zeehond", "otter", "bever", "pinguin", "ooievaar"}
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


def controleer(plekken):
    tel = {}
    for p in plekken:
        tel[p["soort"]] = tel.get(p["soort"], 0) + p["volwassenen"] + p["jongen"]
    for soort, (_, n, _, _) in SOORTEN.items():
        assert (tel.get(soort) or 0) >= n, (soort, tel.get(soort), n)   # zone modules may add families (round 2)
    assert "gouden_dikdik" not in tel
    return sum(tel.values())
