"""
1.7.0 (ronde 45-47, "Vadskracht in de structuren"; Java: feature/techbeurs/VadsschakelaarBlock): the Vadsschakelaar, a
little lever IN a line of Guhdraad (on: the vadskracht goes through, off: everything behind it sleeps). Every stand of the
Guhtechniek-beurs has one; players can craft it too (a lever and a Guhdraad).

Its textures (a pink metal base with a guh face: happy when on, asleep when off; the lever's knob), the models (base + a
tilted lever), the blockstate (facing x powered), item, loot, tags (guhs:vadskracht so Guhdraad bends towards it and the
hover readout works, mineable/pickaxe), the recipe and the texts (nl + en, English overlay c205_techbeurs.json).

Standalone and idempotent:   python tools/features/techbeurs_blokken.py     (from the project root)
"""
import json
import os

import numpy as np
from PIL import Image

R = os.path.join("src", "main", "resources")
A, D = os.path.join(R, "assets", "guhs"), os.path.join(R, "data", "guhs")
OVERLAY = os.path.join("tools", "lang", "en", "c205_techbeurs.json")
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}

METAAL = (214, 98, 160)
METAAL_LICHT = (240, 150, 196)
METAAL_DONKER = (150, 56, 112)
OOG = (58, 28, 60)
BLOS = (255, 120, 168)
HOUT = (196, 150, 110)
HOUT_DONKER = (140, 100, 70)
KNOP_AAN = (120, 230, 140)
KNOP_UIT = (230, 110, 120)


def _w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _add_tag(path, values):
    full = os.path.join(R, "data", path + ".json")
    data = json.load(open(full, encoding="utf-8")) if os.path.exists(full) else {"replace": False, "values": []}
    for v in values:
        if v not in data["values"]:
            data["values"].append(v)
    _w(full, data)


def sheet(aan):
    """16 x 16: (0,0) the top of the base 10 x 10, (0,10) its side 10 x 4 with the face (front), (10,10) a plain side 6 x 4,
    (10,0) the lever stick 2 x 7, (12,0) the knob 3 x 3."""
    rng = np.random.default_rng(4505 if aan else 4506)
    a = np.zeros((16, 16, 4), np.uint8)
    for y in range(16):
        for x in range(16):
            v = int(rng.integers(-7, 8))
            a[y, x, :3] = [max(0, min(255, c + v)) for c in METAAL]
            a[y, x, 3] = 255
    for y in range(10):
        for x in range(10):
            if x in (0, 9) or y in (0, 9):
                a[y, x, :3] = METAAL_DONKER
            elif x in (1, 8) or y in (1, 8):
                a[y, x, :3] = METAAL_LICHT
    a[3:7, 3:7, :3] = METAAL_DONKER          # the slot the lever stands in
    a[4:6, 4:6, :3] = (90, 40, 80)
    # the front side with the face (10 x 4, y 10..13)
    a[10, 0:10, :3] = METAAL_LICHT
    a[13, 0:10, :3] = METAAL_DONKER
    if aan:
        for x in (2, 7):
            a[11, x, :3] = OOG
            a[12, x, :3] = OOG
        a[12, 1, :3] = BLOS
        a[12, 8, :3] = BLOS
        a[12, 4:6, :3] = OOG
    else:
        for x in (2, 7):
            a[12, x - 1:x + 1, :3] = OOG if x == 2 else OOG
        a[12, 4:6, :3] = (110, 60, 100)
    a[10, 10:16, :3] = METAAL_LICHT
    a[13, 10:16, :3] = METAAL_DONKER
    # the stick and the knob
    a[0:7, 10:12, :3] = HOUT
    a[0:7, 11, :3] = HOUT_DONKER
    knop = KNOP_AAN if aan else KNOP_UIT
    a[0:3, 12:15, :3] = knop
    a[0, 12, :3] = (255, 255, 255)
    return Image.fromarray(a)


def model(staat, aan):
    t = "#t"
    basis = {"from": [3, 0, 3], "to": [13, 4, 13], "faces": {
        "up": {"texture": t, "uv": [0, 0, 10, 10]}, "down": {"texture": t, "uv": [0, 0, 10, 10], "cullface": "down"},
        "north": {"texture": t, "uv": [0, 10, 10, 14]}, "south": {"texture": t, "uv": [0, 10, 10, 14]},
        "east": {"texture": t, "uv": [10, 10, 16, 14]}, "west": {"texture": t, "uv": [10, 10, 16, 14]}}}
    draai = {"angle": -22.5 if aan else 22.5, "axis": "x", "origin": [8, 4, 8]}
    stok = {"from": [7, 4, 7], "to": [9, 11, 9], "rotation": draai,
            "faces": {f: {"texture": t, "uv": [10, 0, 12, 7]} for f in ("north", "south", "east", "west")}}
    knop = {"from": [6.5, 11, 6.5], "to": [9.5, 14, 9.5], "rotation": draai,
            "faces": {f: {"texture": t, "uv": [12, 0, 15, 3]} for f in ("north", "south", "east", "west", "up", "down")}}
    return {"parent": "minecraft:block/block", "textures": {"t": f"guhs:block/vadsschakelaar_{staat}",
                                                            "particle": f"guhs:block/vadsschakelaar_{staat}"},
            "elements": [basis, stok, knop]}


TEKSTEN = {  # key: (English, Dutch)
    "block.guhs.vadsschakelaar": ("Chonk Switch", "Vadsschakelaar"),
    "block.guhs.vadsschakelaar.lore": ("Put it in a line of Guh Wire: on, the chonk power goes through; off, everything behind it sleeps",
                                       "Zet hem in een lijn Guhdraad: aan gaat de vadskracht erdoor, uit slaapt alles erachter"),
    "block.guhs.vadsschakelaar.aan": ("Switch on: the chonk power goes through", "Schakelaar aan: de vadskracht gaat erdoor"),
    "block.guhs.vadsschakelaar.uit": ("Switch off: everything behind it sleeps", "Schakelaar uit: alles erachter slaapt"),
}


def build():
    tex = os.path.join(A, "textures", "block")
    os.makedirs(tex, exist_ok=True)
    for staat, aan in (("aan", True), ("uit", False)):
        sheet(aan).save(os.path.join(tex, f"vadsschakelaar_{staat}.png"))
        _w(os.path.join(A, "models", "block", f"vadsschakelaar_{staat}.json"), model(staat, aan))
    _w(os.path.join(A, "blockstates", "vadsschakelaar.json"), {"variants": {
        f"facing={f},powered={str(p).lower()}": {"model": f"guhs:block/vadsschakelaar_{'aan' if p else 'uit'}", **({"y": r} if r else {})}
        for f, r in ROT.items() for p in (False, True)}})
    _w(os.path.join(A, "models", "item", "vadsschakelaar.json"), {"parent": "guhs:block/vadsschakelaar_aan"})
    _w(os.path.join(A, "items", "vadsschakelaar.json"), {"model": {"type": "minecraft:model", "model": "guhs:item/vadsschakelaar"}})
    _w(os.path.join(D, "loot_table", "blocks", "vadsschakelaar.json"), {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "guhs:vadsschakelaar"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}], "random_sequence": "guhs:blocks/vadsschakelaar"})
    _add_tag("guhs/tags/block/vadskracht", ["guhs:vadsschakelaar"])
    _add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:vadsschakelaar"])
    _w(os.path.join(D, "recipe", "vadsschakelaar.json"), {"type": "minecraft:crafting_shaped", "category": "redstone",
                                                          "pattern": ["L", "D"], "key": {"L": "minecraft:lever", "D": "guhs:guh_wire"},
                                                          "result": {"id": "guhs:vadsschakelaar", "count": 1}})
    for taal, i in (("nl_nl", 1), ("en_us", 0)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update({k: v[i] for k, v in TEKSTEN.items()})
        _w(p, data)
    overlay = json.load(open(OVERLAY, encoding="utf-8")) if os.path.exists(OVERLAY) else {}
    overlay.update({k: v[0] for k, v in TEKSTEN.items()})
    _w(OVERLAY, dict(sorted(overlay.items())))
    print(f"techbeurs_blokken: {len(TEKSTEN)} texts")


if __name__ == "__main__":
    build()
