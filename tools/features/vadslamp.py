"""
1.7.0 (design: MineScapeStory/ontwerp-guhs170-mijnen.md ronde 45-47 "Vadskracht in de structuren"; Java:
feature/techbeurs/VadslampBlock + VadslampBlockEntity + VadslampLicht): the Vadslamp, a guh lantern on 1 vadskracht that
burns at light level 15 and keeps invisible light blocks (guhs:vadslamp_licht) in the air around it, so it lights a much
bigger area than a normal lamp. Asleep (a dim lilac glass, closed eyes) without vadskracht, a happy glowing face with it.

Its textures (one 16 x 16 sheet per state: front glass with the face, side glass, the pink metal cap and base, the ears),
the models (a lantern: base, glass body, cap, two ears), the blockstate (facing x snoet, like every machine), the item, the
loot, the tags (guhs:vadskracht for the hover readout, mineable/pickaxe), the recipe (a lantern, two Guhdraad and two
glowstone dust), the texts (nl + en, and the English overlay tools/lang/en/c205_techbeurs.json).

Standalone and idempotent (writes its own files, adds its values to shared tags, merges its texts into nl_nl/en_us):

    python tools/features/vadslamp.py        (from the project root)
"""
import json
import os
import sys

import numpy as np
from PIL import Image

R = os.path.join("src", "main", "resources")
A, D = os.path.join(R, "assets", "guhs"), os.path.join(R, "data", "guhs")
OVERLAY = os.path.join("tools", "lang", "en", "c205_techbeurs.json")
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}

# colours
METAAL = (214, 98, 160)
METAAL_LICHT = (240, 150, 196)
METAAL_DONKER = (150, 56, 112)
OOR = (255, 186, 214)
OOR_BINNEN = (255, 140, 182)
OOG = (58, 28, 60)
BLOS = (255, 120, 168)
GLAS_AAN = (255, 232, 150)
GLAS_AAN_MIDDEN = (255, 250, 214)
GLAS_UIT = (128, 108, 150)
GLAS_UIT_MIDDEN = (150, 130, 170)


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


# =====================================================================================================================
# textures
# =====================================================================================================================
def _glas(a, x0, y0, aan, gezicht):
    """A 6 x 7 glass pane (front with the face, or side) at (x0, y0): a frame of metal, the glass, a glowing middle."""
    rand, glas, midden = METAAL_DONKER, GLAS_AAN if aan else GLAS_UIT, GLAS_AAN_MIDDEN if aan else GLAS_UIT_MIDDEN
    for y in range(7):
        for x in range(6):
            kleur = glas
            if x in (0, 5) or y in (0, 6):
                kleur = rand
            elif 2 <= x <= 3 and 2 <= y <= 4 and not gezicht:
                kleur = midden
            a[y0 + y, x0 + x, :3] = kleur
            a[y0 + y, x0 + x, 3] = 255
    if gezicht:
        if aan:     # happy: open eyes, rosy cheeks, a little smile; the glass glows around them
            for p in ((1, 2), (4, 2), (1, 3), (4, 3)):
                a[y0 + p[1], x0 + p[0], :3] = OOG
            a[y0 + 4, x0 + 1, :3] = BLOS
            a[y0 + 4, x0 + 4, :3] = BLOS
            a[y0 + 5, x0 + 2, :3] = OOG
            a[y0 + 5, x0 + 3, :3] = OOG
            a[y0 + 1, x0 + 2, :3] = GLAS_AAN_MIDDEN
            a[y0 + 1, x0 + 3, :3] = GLAS_AAN_MIDDEN
        else:       # sleepy: closed eyes (low little dashes), a tiny snoet
            for p in ((1, 3), (4, 3)):
                a[y0 + p[1], x0 + p[0], :3] = OOG
            a[y0 + 5, x0 + 2, :3] = (100, 70, 110)
            a[y0 + 5, x0 + 3, :3] = (100, 70, 110)


def sheet(aan):
    """The 16 x 16 sheet: (0,0) front glass 6 x 7, (6,0) side glass 6 x 7, (12,0) ear 2 x 2 + its inside (14,0),
    (0,8) metal top 8 x 8, (8,8) cap side 8 x 2, (8,10) base side 8 x 2, (8,12) a plain metal patch (particle)."""
    a = np.zeros((16, 16, 4), np.uint8)
    rng = np.random.default_rng(1707 if aan else 1708)
    _glas(a, 0, 0, aan, True)
    _glas(a, 6, 0, aan, False)
    a[0:2, 12:14, :3] = OOR
    a[0:2, 14:16, :3] = OOR_BINNEN
    a[0:2, 12:16, 3] = 255
    for y in range(8, 16):
        for x in range(16):
            v = int(rng.integers(-8, 9))
            a[y, x, :3] = [max(0, min(255, c + v)) for c in METAAL]
            a[y, x, 3] = 255
    # the top: a dark rim, a lighter ring, a little hole in the middle
    for y in range(8):
        for x in range(8):
            if x in (0, 7) or y in (0, 7):
                a[8 + y, x, :3] = METAAL_DONKER
            elif x in (1, 6) or y in (1, 6):
                a[8 + y, x, :3] = METAAL_LICHT
    a[11:13, 3:5, :3] = METAAL_DONKER
    # cap and base sides: a light top edge, a dark bottom edge, rivets
    for y0 in (8, 10):
        a[y0, 8:16, :3] = METAAL_LICHT
        a[y0 + 1, 8:16, :3] = METAAL_DONKER
        a[y0 + 1, 9, :3] = METAAL_LICHT
        a[y0 + 1, 14, :3] = METAAL_LICHT
    return Image.fromarray(a)


# =====================================================================================================================
# models
# =====================================================================================================================
def _vlakken(uv, faces=("north", "south", "east", "west", "up", "down"), extra=None):
    out = {f: {"texture": "#t", "uv": uv} for f in faces}
    for f, u in (extra or {}).items():
        out[f] = {"texture": "#t", "uv": u}
    return out


def model(staat):
    top = [0, 8, 8, 16]
    return {"parent": "minecraft:block/block",
            "textures": {"t": f"guhs:block/vadslamp_{staat}", "particle": f"guhs:block/vadslamp_{staat}"},
            "elements": [
                {"from": [4, 0, 4], "to": [12, 2, 12], "faces": _vlakken([8, 10, 16, 12], extra={"up": top, "down": top})},
                {"from": [5, 2, 5], "to": [11, 9, 11], "faces": _vlakken([6, 0, 12, 7], faces=("south", "east", "west"),
                                                                          extra={"north": [0, 0, 6, 7]})},
                {"from": [4, 9, 4], "to": [12, 11, 12], "faces": _vlakken([8, 8, 16, 10], extra={"up": top, "down": top})},
                {"from": [5, 11, 5], "to": [7, 13, 7], "faces": _vlakken([12, 0, 14, 2], extra={"north": [14, 0, 16, 2]})},
                {"from": [9, 11, 5], "to": [11, 13, 7], "faces": _vlakken([12, 0, 14, 2], extra={"north": [14, 0, 16, 2]})},
                {"from": [7, 11, 7.5], "to": [9, 12, 8.5], "faces": _vlakken([8, 8, 10, 9])},
            ]}


# =====================================================================================================================
# texts
# =====================================================================================================================
TEKSTEN = {  # key: (English, Dutch)
    "block.guhs.vadslamp": ("Chonk Lamp", "Vadslamp"),
    "block.guhs.vadslamp.lore": ("On 1 chonk power: light level 15, and it lights up a much bigger area than a normal lamp",
                                 "Op 1 vadskracht: lichtniveau 15, en hij verlicht een veel groter stuk dan een gewone lamp"),
    "block.guhs.vadslamp_licht": ("Chonk Lamp Light", "Licht van een Vadslamp"),
}


def build():
    tex = os.path.join(A, "textures", "block")
    os.makedirs(tex, exist_ok=True)
    for staat, aan in (("aan", True), ("uit", False)):
        sheet(aan).save(os.path.join(tex, f"vadslamp_{staat}.png"))
        _w(os.path.join(A, "models", "block", f"vadslamp_{staat}.json"), model(staat))
    _w(os.path.join(A, "blockstates", "vadslamp.json"), {"variants": {
        f"facing={f},snoet={s}": {"model": f"guhs:block/vadslamp_{'uit' if s == 'slaapt' else 'aan'}", **({"y": r} if r else {})}
        for f, r in ROT.items() for s in ("slaapt", "werkt", "vol")}})
    _w(os.path.join(A, "models", "item", "vadslamp.json"), {"parent": "guhs:block/vadslamp_aan"})
    _w(os.path.join(A, "items", "vadslamp.json"), {"model": {"type": "minecraft:model", "model": "guhs:item/vadslamp"}})
    # the light: nothing to see
    _w(os.path.join(A, "models", "block", "vadslamp_licht.json"), {"textures": {"particle": "guhs:block/vadslamp_aan"}})
    _w(os.path.join(A, "blockstates", "vadslamp_licht.json"), {"variants": {"": {"model": "guhs:block/vadslamp_licht"}}})
    # loot, tags, recipe
    _w(os.path.join(D, "loot_table", "blocks", "vadslamp.json"), {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "guhs:vadslamp"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}], "random_sequence": "guhs:blocks/vadslamp"})
    _add_tag("guhs/tags/block/vadskracht", ["guhs:vadslamp"])
    _add_tag("minecraft/tags/block/mineable/pickaxe", ["guhs:vadslamp"])
    _w(os.path.join(D, "recipe", "vadslamp.json"), {"type": "minecraft:crafting_shaped", "category": "redstone",
                                                    "pattern": [" G ", "DLD", " G "],
                                                    "key": {"G": "minecraft:glowstone_dust", "D": "guhs:guh_wire", "L": "minecraft:lantern"},
                                                    "result": {"id": "guhs:vadslamp", "count": 1}})
    # texts
    for taal, i in (("nl_nl", 1), ("en_us", 0)):
        p = os.path.join(A, "lang", taal + ".json")
        data = json.load(open(p, encoding="utf-8"))
        data.update({k: v[i] for k, v in TEKSTEN.items()})
        _w(p, data)
    overlay = json.load(open(OVERLAY, encoding="utf-8")) if os.path.exists(OVERLAY) else {}
    overlay.update({k: v[0] for k, v in TEKSTEN.items()})
    _w(OVERLAY, dict(sorted(overlay.items())))
    test_kamer()
    print(f"vadslamp: {len(TEKSTEN)} texts")


def test_kamer():
    """techbeurs/vadslamp_kamer (game tests): 15 x 9 x 15, a stone floor (things stand at y 1), a stone wall at x 10 (z 4..10,
    y 1..4) that stops the lamp's rays."""
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    import make_structures as ms
    s = ms.Structure((15, 9, 15))
    s.fill(0, 0, 0, 14, 0, 14, "minecraft:stone")
    s.fill(10, 1, 4, 10, 4, 10, "minecraft:stone")
    s.save("techbeurs/vadslamp_kamer")


if __name__ == "__main__":
    build()
