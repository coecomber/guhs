"""
Sleeping guhs close their eyes: textures/entity/guh_slaap/<texture>.png is the guh's fur texture with the two big eyes
painted over with fur (taken from the top of the head, so stars and speckles stay) and two closed eyes drawn on (a soft
curve with three little lashes). GuhRenderer uses it while a guh sleeps (the SLAPEN emote, or asleep in a guh nest).
The Enderguh's glowmask has glowing eyes: guh_slaap/guh_ender_glowmask.png is the same mask without them.

    python tools/make_sleep_eyes.py
"""
import os

from PIL import Image, ImageDraw

ENTITY = os.path.join("src", "main", "resources", "assets", "guhs", "textures", "entity")
OUT = os.path.join(ENTITY, "guh_slaap")
TEXTURES = ["guh"] + [f"guh_{v}" for v in (
    "asguh", "brococolief", "brontosaurus", "choco", "ender", "ghost", "golden", "kaasmoerasguh", "koning", "mager", "mint",
    "pluisguh", "snow", "starry", "teckel", "vahoege_ender", "wolk", "zeemeerguh",
    "pinguh", "pinguh_keizer", "pinguh_pluis",
    "baltoguh", "mewtwo", "stitch626")] + [f"guh_rainbow_{i}" for i in range(8)]
# (3.0: "baltoguh", "mewtwo", "stitch626": the story guhs of the Guhverhalen, painted by make_guh_variants + their slices)
# (2.9: "pinguh", "pinguh_keizer", "pinguh_pluis" are the three Pinguh looks, painted by tools/features/guhpolder.py)
GLOWMASKS = ["guh_ender_glowmask"]

S = 4                                   # texture pixels per model UV unit (512 px texture, 128 UV)
FACE = (0, 49 * S, 14 * S, 60 * S)      # the head's front (north) face
TOP = (52 * S, 49 * S)                  # the head's top face: clean fur to paint over the eyes with
EYES = [(0, 26), (30, 56)]              # the x ranges of the two eyes on the face
EYE_Y = (FACE[1], FACE[1] + 24)         # (the nose starts right under them)


def fur_colour(img):
    """The most common colour of the face: the fur."""
    px = [img.getpixel((x, y)) for x in range(FACE[0], FACE[2]) for y in range(FACE[1] + 28, FACE[3])]
    px = [p for p in px if p[3] > 0]
    return tuple(sorted(px, key=lambda p: sum(p[:3]))[len(px) // 2])


def far(a, b, limit=60):
    return sum(abs(x - y) for x, y in zip(a[:3], b[:3])) > limit


def eye_boxes(img, fur):
    boxes = []
    for x0, x1 in EYES:
        pts = [(x, y) for x in range(x0, x1) for y in range(max(FACE[1], EYE_Y[0]), EYE_Y[1]) if far(img.getpixel((x, y)), fur)]
        if pts:
            boxes.append((min(p[0] for p in pts), min(p[1] for p in pts), max(p[0] for p in pts), max(p[1] for p in pts)))
    return boxes


def closed_eye(img, box, colour):
    """A soft 'u' curve over the lower half of the eye, 3 px thick, and three little lashes under it."""
    x0, y0, x1, y1 = box
    cx, w = (x0 + x1) / 2, (x1 - x0) / 2 - 1.5
    base, depth = y0 + (y1 - y0) * 0.38, 6.0
    curve = lambda t: (cx + t * w, base + depth * (1 - t * t))  # noqa: E731
    d = ImageDraw.Draw(img)
    d.line([curve(i / 12 - 1) for i in range(25)], fill=colour, width=3, joint="curve")
    for t in (-0.6, 0.0, 0.6):   # lashes, pointing down and a little outwards
        x, y = curve(t)
        d.line([(x, y + 1), (x + t * 3, y + 5)], fill=colour, width=2)


def pinguh_face(img, fur, y):
    """A Pinguh: the colour between its eyes on that row (its dark cap at the top, else its white face)."""
    mid = img.getpixel(((FACE[0] + FACE[2]) // 2, y))
    return mid if far(mid, fur) and y < FACE[1] + 10 else fur   # (the cap is only the top rows; the mouth is lower)


def sleepy(name):
    img = Image.open(os.path.join(ENTITY, name + ".png")).convert("RGBA")
    fur = fur_colour(img)
    boxes = eye_boxes(img, fur)
    assert len(boxes) == 2, (name, boxes)
    for x0, y0, x1, y1 in boxes:   # fur over the eyes (the same spot on the head's top face)
        for x in range(x0 - 1, x1 + 2):
            for y in range(y0 - 1, y1 + 2):
                if FACE[0] <= x < FACE[2] and FACE[1] <= y < FACE[3]:
                    # (a Pinguh's head top is dark but its face is white: paint its eyes over with the face colour)
                    img.putpixel((x, y), pinguh_face(img, fur, y) if name.startswith("guh_pinguh") else img.getpixel((TOP[0] + x - FACE[0], TOP[1] + y - FACE[1])))
    lash = (38, 22, 40, 255)
    for box in boxes:
        closed_eye(img, box, lash)
    img.save(os.path.join(OUT, name + ".png"), optimize=True)
    return boxes


def main():
    os.makedirs(OUT, exist_ok=True)
    boxes = None
    for name in TEXTURES:
        b = sleepy(name)
        boxes = boxes or b
    for name in GLOWMASKS:   # the same mask without the eyes (they're closed, so they don't glow)
        img = Image.open(os.path.join(ENTITY, name + ".png")).convert("RGBA")
        for x0, y0, x1, y1 in boxes:
            for x in range(x0 - 1, x1 + 2):
                for y in range(max(FACE[1], y0 - 1), y1 + 2):
                    img.putpixel((x, y), (0, 0, 0, 0))
        img.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("sleeping eyes:", len(TEXTURES), "textures +", len(GLOWMASKS), "glowmask in", OUT)


if __name__ == "__main__":
    main()
