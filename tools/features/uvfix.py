"""
3.0 visual QA: block model elements that stick out of the 0..16 block get automatic uvs outside their texture, so they show
bits of other textures from the atlas. binnen(elements) gives such faces explicit uvs of the same size, moved onto the
texture (faces that already have a uv are left alone).
"""


def _auto(face, fr, to):
    x0, y0, z0 = fr
    x1, y1, z1 = to
    return {
        "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "south": [x0, 16 - y1, x1, 16 - y0],
        "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0], "west": [z0, 16 - y1, z1, 16 - y0],
        "up": [x0, z0, x1, z1], "down": [x0, 16 - z1, x1, 16 - z0],
    }[face]


def _schuif(a, b):
    """Moves the range a..b (at most 16 long) into 0..16."""
    lengte = min(b - a, 16)
    if a < 0:
        a = 0
    if a + lengte > 16:
        a = 16 - lengte
    return round(a, 4), round(a + lengte, 4)


def binnen(elements):
    for e in elements:
        fr, to = e["from"], e["to"]
        if min(fr + to) >= 0 and max(fr + to) <= 16:
            continue
        for face, f in e.get("faces", {}).items():
            if "uv" in f:
                continue
            u0, v0, u1, v1 = _auto(face, fr, to)
            u0, u1 = _schuif(u0, u1)
            v0, v1 = _schuif(v0, v1)
            f["uv"] = [u0, v0, u1, v1]
    return elements
