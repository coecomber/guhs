"""
1.4.2 (fixes): repair lists for structures whose template got a fix, so the copies built BEFORE the fix are mended by
the game (J/feature/wereld/Reparaties.java reads assets/guhs/herstel/<id>.json). Not a feature module (not in FEATURES):
the builders that changed a template call schrijf() with the spots they changed.

A spot: template coordinates, the block ids that stood there before the fix (only those are replaced in the world), and
the block that stands there now (read from the finished template, so file and template always agree).
"""


def _id(name):
    return name if ":" in name else "minecraft:" + name


def schrijf(h, herstel_id, structuur, s, plekken, stuk=None):
    """plekken: [((x, y, z), [old block ids])]; s: the finished Structure."""
    blokken = []
    for (pos, oud) in plekken:
        blk = s.blocks.get(tuple(pos))
        if blk is None:
            raise SystemExit(f"herstel {herstel_id}: nothing in the template at {pos}")
        name, props, _nbt = blk
        if _id(name) in [_id(o) for o in oud]:
            raise SystemExit(f"herstel {herstel_id}: {pos} still holds {name}, the block it should replace")
        e = {"pos": list(pos), "oud": sorted({_id(o) for o in oud}), "blok": _id(name)}
        if props:
            e["props"] = {k: str(v) for k, v in sorted(props.items())}
        blokken.append(e)
    out = {"structuur": structuur, "blokken": blokken}
    if stuk:
        out["stuk"] = stuk
    h.w(f"{h.A}/herstel/{herstel_id}.json", out)
    return blokken
