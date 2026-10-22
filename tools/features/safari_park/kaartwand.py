"""
Test round 5 (FX-A): the kaartwand in the entreegebouw. The "park van boven" model did not come across; now the wall
shows the real park on 6 x 6 Minecraft maps (1 block per pixel, together the whole 768 x 768 park world), drawn from the
finished park blocks. Written to data/guhs/safari/kaartwand.json; the game (feature/safari/Kaartwand) makes the map data
and hangs the maps in fixed item frames when the park is stamped.

  borden(park)     before the tiles are written: the column signs under the maps (the zones from north to south)
  schrijf(root, park, log)   after the check: the map pixels

Per map: the palette index of the top visible block per column (glass and other see-through blocks skipped, like the
game's maps) * 4 + the brightness (the game's map shading: the height step to the north; water: its depth), as uint16,
zlib + base64. The game turns a palette name into its MapColor.
"""
import base64
import json
import os
import zlib

import numpy as np

from . import kit
from . import zone_entree as E
from .terrein import ZONES

N = 6
MAAT = 128
ONZICHTBAAR = ("air", "cave_air", "void_air", "glass", "glass_pane", "barrier", "light", "structure_void", "tripwire",
               "string", "torch", "wall_torch", "soul_torch", "redstone_torch", "kasglas", "terrariumglas", "glasvloer")
NAAM = {"savanne": "Savanne", "delta": "Delta", "regenwoud": "Regenwoud", "bamboe": "Bamboebos", "ijskust": "IJskust",
        "oertijd": "Oertijd", "europa": "Europa", "australie": "Australie", "entree": "Lobby"}


def _plekken():
    kw = E.KAARTWAND
    if not kw:
        return None
    lijst = []
    for i in range(N):
        for j in range(N):
            x, y, z = kw["x0"] + i, kw["y0"] - j, kw["z"] + 1
            lijst.append((i, j, (E.OX + x, E.OY + y, E.OZ + z)))
    return lijst


def borden(park):
    """The six column signs under the maps: the zones in that column of maps, north to south (the lobby too)."""
    kw = E.KAARTWAND
    if not kw:
        return
    for i in range(N):
        namen = []
        for j in range(N):
            blok = park.zone[i * MAAT:(i + 1) * MAAT, j * MAAT:(j + 1) * MAAT]
            binnen = blok[blok >= 0]
            if binnen.size < MAAT * MAAT * 0.08:
                continue
            naam = NAAM.get(ZONES[int(np.bincount(binnen).argmax())], "")
            if naam and naam not in namen:
                namen.append(naam)
        regels = namen[:4] if len(namen) <= 4 else [" / ".join(namen[k:k + 2]) for k in range(0, len(namen), 2)][:4]
        x, y, z = kw["x0"] + i, E.G + 2, kw["z"] + 1
        park.bordjes.append({"pos": [E.OX + x, E.OY + y, E.OZ + z], "tekst": regels or ["", "(decor)", "", ""]})


def schrijf(root, park, log=print):
    plekken = _plekken()
    if plekken is None:
        log("    kaartwand: no kaartwand in the lobby, nothing written")
        return
    W, H = kit.W, kit.H
    namen = [s.split("[")[0] for s in park.pal]
    zichtbaar = np.array([n.split(":")[-1] not in ONZICHTBAAR for n in namen], bool)
    is_water = np.array([n == "minecraft:water" for n in namen], bool)
    v = park.v
    zm = zichtbaar[v]                                                  # [x, y, z]
    heeft = zm.any(axis=1)
    top = np.where(heeft, H - 1 - np.argmax(zm[:, ::-1, :], axis=1), 0)
    xi, zi = np.meshgrid(np.arange(W), np.arange(W), indexing="ij")
    blok = v[xi, top, zi]
    blok[~heeft] = 0
    water = is_water[blok]
    # water depth: water blocks under the top
    diepte = np.zeros((W, W), np.int32)
    wm = is_water[v]
    for d in range(1, 12):
        y = np.clip(top - d, 0, H - 1)
        diepte += (water & wm[xi, y, zi] & (diepte == d - 1)).astype(np.int32)
    gebruikt = np.unique(blok)
    pal_idx = {int(b): k for k, b in enumerate(gebruikt)}
    palet = [park.pal[int(b)] for b in gebruikt]
    lut = np.zeros(int(gebruikt.max()) + 1, np.int32)
    for b, k in pal_idx.items():
        lut[b] = k
    idx = lut[blok]
    h = top.astype(np.float64)
    noord = np.vstack([h[:, :1].T, h[:, :-1].T]).T                     # height of the column to the north (z - 1)
    pariteit = ((xi + zi) & 1).astype(np.float64)
    d_land = (h - noord) + (pariteit - 0.5) * 0.4
    helder = np.where(d_land > 0.6, 2, np.where(d_land < -0.6, 0, 1))
    d_water = diepte * 0.1 + pariteit * 0.2
    helder_w = np.where(d_water < 0.5, 2, np.where(d_water > 0.9, 0, 1))
    helder = np.where(water, helder_w, helder)
    pix = (idx * 4 + helder).astype("<u2")
    kaarten = []
    for (i, j, frame) in plekken:
        stuk = pix[i * MAAT:(i + 1) * MAAT, j * MAAT:(j + 1) * MAAT]           # [x, z]
        rij = np.ascontiguousarray(stuk.T)                                    # map colours are row-major: z * 128 + x
        kaarten.append({"kolom": i, "rij": j, "frame": list(frame), "midden": [i * MAAT + 64, j * MAAT + 64],
                        "pixels": base64.b64encode(zlib.compress(rij.tobytes(), 9)).decode("ascii")})
    data = {"versie": 1, "schaal": 0, "facing": "south", "palet": palet, "kaarten": kaarten,
            "uitleg": "round 5: the kaartwand in the entreegebouw; pixels = uint16 LE, palette index * 4 + brightness "
                      "(0 low, 1 normal, 2 high), z * 128 + x"}
    pad = os.path.join(root, kit.OUT_DATA, "kaartwand.json")
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, separators=(",", ":"))
        f.write("\n")
    log(f"    kaartwand: {len(kaarten)} maps, {len(palet)} colours -> {pad} ({os.path.getsize(pad) // 1024} KB)")
