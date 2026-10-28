"""
1.8.0 afronding "weerrest": a little chest in four places that had none, so their weerpotje can hide there too
(the loot modifiers of tools/features/weer.py add the potje to these tables, about 1 in 8):

  wolkenhoeder_hut  next to the barrel in the Wolkenhoeder's hut        -> pluisjessneeuw
  regenboogbrug     next to the pot at the end of the rainbow           -> regenboogweer
  knuffelbad        next to the tafel with the guh_taart                -> knusmist
  dal_theehuisje    next to the theetafel of the Klaterdal tea house    -> bloesemwind

The templates are patched in place (tools/hekjes.py reader/writer, the rest of each template stays byte for byte) and the
patch is idempotent: a template that already has the chest is left alone. Only newly generated places get the chest.

    PYTHONPATH=tools python tools/features/weerrest.py
"""
import gzip
import json
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
import hekjes as h  # noqa: E402

STRUCT = os.path.join("src", "main", "resources", "data", "guhs", "structure")
LOOT = os.path.join("src", "main", "resources", "data", "guhs", "loot_table", "chests", "weer")

# template -> (anchor block, loot table id, a themed item for the chest)
PLEKKEN = {
    "wolkenhoeder_hut": ("minecraft:barrel", "guhs:chests/weer/wolkenhoeder_hut", "guhs:wolkenblok_wit"),
    "regenboogbrug": ("guhs:regenboogbrug_pot", "guhs:chests/weer/regenboogbrug", "guhs:regenboogblok"),
    "knuffelbad": ("guhs:guh_taart", "guhs:chests/weer/knuffelbad", "guhs:pink_kussen"),
    "dal_theehuisje": ("guhs:theetafel", "guhs:chests/weer/dal_theehuisje", "guhs:theepotje"),
}
ZIJDEN = {(0, -1): "north", (0, 1): "south", (-1, 0): "west", (1, 0): "east"}
LEEG = {"minecraft:air", "minecraft:cave_air", "minecraft:structure_void"}
ZACHT = ("carpet", "flower", "grass", "sapling", "button", "torch", "lantern", "lampion", "sign", "rail")


def loot_table(themed):
    """A small, sweet chest: kaasknabbels, a roze guh koek now and then, and something of the place itself."""
    return {
        "type": "minecraft:chest",
        "pools": [
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:kaas_knabbels",
                                      "functions": [{"function": "minecraft:set_count", "count": {"min": 2, "max": 5}}]}]},
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": themed,
                                      "functions": [{"function": "minecraft:set_count", "count": {"min": 1, "max": 4}}]},
                                     {"type": "minecraft:empty", "weight": 1}]},
            {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "guhs:roze_guh_koek"},
                                     {"type": "minecraft:empty", "weight": 3}]},
        ],
    }


def _str(s):
    return h.Tag(8, s.encode("utf-8"))


def patch(naam, anker, tabel):
    pad = os.path.join(STRUCT, naam + ".nbt")
    name, root, _ = h.lees(pad)
    c = root.v
    palette, blocks = c[b"palette"].v[1], c[b"blocks"].v[1]
    staat = [h._s(e[b"Name"]) for e in palette]
    for b in blocks:   # already there?
        if staat[b[b"state"].v] == "minecraft:chest" and b"nbt" in b and b"LootTable" in b[b"nbt"].v \
                and h._s(b[b"nbt"].v[b"LootTable"]) == tabel:
            return "al klaar"
    wereld = {tuple(b[b"pos"].v[1]): staat[b[b"state"].v] for b in blocks}
    sx, sy, sz = c[b"size"].v[1]
    ankers = [p for p, n in wereld.items() if n == anker]
    if not ankers:
        raise SystemExit(f"{naam}: anchor {anker} not found")
    ax, ay, az = ankers[0]

    def vrij(p):
        return 0 <= p[0] < sx and 0 <= p[1] < sy and 0 <= p[2] < sz and wereld.get(p, "minecraft:air") in LEEG

    def vast(p):
        n = wereld.get(p, "minecraft:air")
        return n not in LEEG and "water" not in n and not any(z in n for z in ZACHT)

    beste = None
    for r in (1, 2, 3):   # the nearest free spot on the same floor as the anchor, with a solid block below
        for dy in (0, -1, 1):
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if max(abs(dx), abs(dz)) != r:
                        continue
                    p = (ax + dx, ay + dy, az + dz)
                    if not (vrij(p) and vast((p[0], p[1] - 1, p[2])) and vrij((p[0], p[1] + 1, p[2]))):
                        continue
                    # face the open side, preferably towards the anchor's side; a wall behind it scores
                    open_zijden = [(d, f) for d, f in ZIJDEN.items() if vrij((p[0] + d[0], p[1], p[2] + d[1]))]
                    if not open_zijden:
                        continue
                    muren = sum(1 for d in ZIJDEN if vast((p[0] + d[0], p[1], p[2] + d[1])))
                    score = (abs(dx) + abs(dz)) * 2 + abs(dy) * 3 - muren
                    if beste is None or score < beste[0]:
                        naar_anker = min(open_zijden, key=lambda df: (df[0][0] - (ax - p[0])) ** 2 + (df[0][1] - (az - p[2])) ** 2)
                        beste = (score, p, naar_anker[1])
        if beste:
            break
    if not beste:
        raise SystemExit(f"{naam}: no free spot next to {anker}")
    _, pos, facing = beste
    palette.append({b"Name": _str("minecraft:chest"),
                    b"Properties": h.Tag(10, {b"facing": _str(facing), b"type": _str("single"), b"waterlogged": _str("false")})})
    idx = len(palette) - 1
    entry = {b"pos": h.Tag(9, (3, list(pos))), b"state": h.Tag(3, idx),
             b"nbt": h.Tag(10, {b"id": _str("minecraft:chest"), b"LootTable": _str(tabel)})}
    for i, b in enumerate(blocks):   # an explicit air entry at that spot is replaced, otherwise appended
        if tuple(b[b"pos"].v[1]) == pos:
            blocks[i] = entry
            break
    else:
        blocks.append(entry)
    with open(pad, "wb") as f:
        f.write(gzip.compress(h.bytes_van(name, root), mtime=0))
    return f"kist op {pos} ({facing})"


def main():
    os.makedirs(LOOT, exist_ok=True)
    for naam, (anker, tabel, themed) in PLEKKEN.items():
        with open(os.path.join(LOOT, tabel.split("/")[-1] + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(loot_table(themed), f, indent=2)
            f.write("\n")
        print(f"weerrest: {naam}: {patch(naam, anker, tabel)}")


if __name__ == "__main__":
    main()
