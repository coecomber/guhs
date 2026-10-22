"""
Op Safari! slice G1 (the gaps of the design audit), ronde 46: the "bekende bewoners" get their little sign. At the home of
each named park animal (the first home of that species in that zone, the same one the game names: gedrag/Bewoners.java),
a standing sign by the nearest path says who lives there, with a funny fact.

Runs after the zones (NA_ZONES) and places the signs in the plekken hook, when the homes are known.
"""
import json
import os

import numpy as np

from . import kit

NA_ZONES = True

# soort_zone -> (naam, two short lines of fun fact); names from tools/features/safari_dieren.py BEWONERS
FEITJES = {
    "olifant_savanne": ("Flappie", "wappert met z'n", "oren tegen hitte"),
    "leeuw_savanne": ("Manenmans", "slaapt 20 uur", "per dag. Rawr..."),
    "giraffe_savanne": ("Langnek Lotte", "tong van een", "halve meter!"),
    "nijlpaard_delta": ("Gapende Gijs", "zweet roze", "zonnebrand"),
    "flamingo_delta": ("Roze Rosa", "roze van het", "garnalen-eten"),
    "luiaard_regenwoud": ("Slome Sjaak", "poept 1x per", "week. Rustig."),
    "toekan_regenwoud": ("Snavel Saar", "snavel is licht", "als een veertje"),
    "reuzenpanda_bamboe": ("Bamboe Bas", "eet 12 uur per", "dag bamboe"),
    "rode_panda_bamboe": ("Pluimpje", "staart = een", "warme sjaal"),
    "pinguin_ijskust": ("Flapje", "glijdt liever", "dan te lopen"),
    "ijsbeer_ijskust": ("Sneeuwvlok", "zwarte huid", "onder de vacht"),
    "mammoet_oertijd": ("Wolbol", "had een vacht", "als een tapijt"),
    "bever_europa": ("Knaagje", "tanden groeien", "altijd door"),
    "otter_europa": ("Otto", "slaapt hand in", "hand met maatjes"),
    "koala_australie": ("Slaapkop Kees", "slaapt 20 uur,", "knuffelt de rest"),
    "kangoeroe_australie": ("Hupsje", "kan niet", "achteruit lopen"),
}
RICHTING = ((0, 1, 0), (0, -1, 8), (1, 0, 12), (-1, 0, 4))      # (dx, dz) of the path next to the sign -> sign rotation


def bouw(park, log=print):
    pass


ZACHT = ("grass", "fern", "flower", "bush", "dandelion", "poppy", "tulip", "daisy", "orchid", "allium", "bluet",
         "cornflower", "lily_of", "guhbloem", "guhoortjes", "snow[")


def _zacht(park, x, y, z):
    """Air, or a plant that a sign may replace."""
    i = int(park.v[x, y, z])
    return i == 0 or any(w in park.pal[i] for w in ZACHT) and "block" not in park.pal[i]


def _pad(park, x, z):
    """A path column: the truck route (park.pad) or a foot path (dirt_path on top)."""
    return bool(park.pad[x, z]) or park.pal[int(park.v[x, int(park.hoogte[x, z]), z])] == "minecraft:dirt_path"


def _plek_bij_huis(park, cx, cz, r0, r1):
    """Fallback (a home deep in the trees, no path near): the closest free dry column on the home's edge, facing out."""
    W = park.hoogte.shape[0]
    beste, bd = None, 1e9
    for x in range(max(1, cx - r1), min(W - 1, cx + r1 + 1)):
        for z in range(max(1, cz - r1), min(W - 1, cz + r1 + 1)):
            d = (x - cx) ** 2 + (z - cz) ** 2
            if d < r0 * r0 or d > r1 * r1 or d >= bd or not park.binnen[x, z] or park.water[x, z]:
                continue
            g = int(park.hoogte[x, z])
            if _zacht(park, x, g + 1, z) and _zacht(park, x, g + 2, z) and park.v[x, g, z] != 0:
                dx, dz = x - cx, z - cz
                rot = (0 if dz > 0 else 8) if abs(dz) >= abs(dx) else (12 if dx > 0 else 4)
                beste, bd = (x, z, rot), d
    return beste


def _plek_voor_bord(park, cx, cz, r0, r1):
    """The closest column (ring r0..r1 around cx, cz) beside a path, free and dry: (x, z, rotation) or None."""
    W = park.hoogte.shape[0]
    beste, bd = None, 1e9
    for x in range(max(1, cx - r1), min(W - 1, cx + r1 + 1)):
        for z in range(max(1, cz - r1), min(W - 1, cz + r1 + 1)):
            d = (x - cx) ** 2 + (z - cz) ** 2
            if d < r0 * r0 or d > r1 * r1 or d >= bd:
                continue
            if not park.binnen[x, z] or _pad(park, x, z) or park.water[x, z]:
                continue
            g = int(park.hoogte[x, z])
            if not _zacht(park, x, g + 1, z) or not (_zacht(park, x, g + 2, z) or "leaves" in park.pal[int(park.v[x, g + 2, z])]) or park.v[x, g, z] == 0:
                continue
            for dx, dz, rot in RICHTING:
                if _pad(park, x + dx, z + dz) and abs(int(park.hoogte[x + dx, z + dz]) - g) <= 2:
                    beste, bd = (x, z, rot), d
                    break
    return beste


def plekken(park, plekken_):
    gezet = 0
    gehad = set()
    for p in plekken_:
        sleutel = f"{p['soort']}_{p.get('zone', '')}"
        if sleutel not in FEITJES or sleutel in gehad:
            continue
        gehad.add(sleutel)
        naam, a, b = FEITJES[sleutel]
        cx, cy, cz = p["centrum"]
        r = int(p.get("straal", 6))
        plek = _plek_voor_bord(park, int(cx), int(cz), r + 1, r + 48) or _plek_bij_huis(park, int(cx), int(cz), r + 1, r + 10)
        if plek is None:
            print(f"    g1: geen bordplek voor {naam} ({sleutel})")
            continue
        x, z, rot = plek
        g = int(park.hoogte[x, z])
        park.set(x, g + 2, z, "air")
        park.set(x, g + 1, z, "oak_sign", rotation=rot, waterlogged="false")
        park.bord_nbt(x, g + 1, z, ["Hier woont", naam + "!", a, b])
        gezet += 1
    print(f"    g1: {gezet}/{len(FEITJES)} bewonersbordjes")
