"""
1.10.0 "Het Vadsrijk", U-ZICHTBAAR: the looks of the gear in the virtual slots (cape, gloves, armband, rings, necklace, earring)
on the player model. Java: feature/vadsrijk/zichtbaar (Looks = the same id lists; keep them in step).
Writes:
  - textures/vadsrijk/zichtbaar/cape_<naam>.png (64x32, vanilla cape layout; drawn through the vanilla cape layer) for every cape
    with its own look, plus cape_standaard (any other cape);
  - item definitions assets/guhs/items/vr_u_zichtbaar_<handschoen|armband>_<naam>.json + models/item/vr_u_zichtbaar/... (cuboids
    around the lower arm, untinted, own 16x16 texture textures/item/vr_u_zichtbaar/<...>.png);
  - generic jewellery: vr_u_zichtbaar_<ring|ketting|oorbel|armband|handschoen>_standaard (gold metal + a white gem that the dye tint
    colours; the colour comes from Looks.kleur).
Geometry: every model is written in the SPACE OF ITS BODY PART (model px, y down, -z = the front) and converted to item px with
X = 8 - mx, Y = -my, Z = mz + 8 (the transform ZichtbaarLaag uses: part pose, Z 180 deg, up 0.5). Arm pieces are centred on the arm
(mx = 0); the layer moves them to the arm centre (wide -1/+1, slim -0.5/+0.5) and narrows them for slim arms.
Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_u_zichtbaar.py
"""
import json
import os
import random

try:
    from PIL import Image, ImageDraw
except ImportError:  # pragma: no cover
    Image = None

A = os.path.join("src", "main", "resources", "assets", "guhs")

# ---------------------------------------------------------------------------------------------------------------------
# the looks: (main colour, second colour, accent) per piece
# ---------------------------------------------------------------------------------------------------------------------
KAPEN = {
    # t-items, zone 1-3
    "knuffelcapeje": (0xFFB8DC, 0xFF7FC0, 0xFFFFFF),
    "sporencape": (0xB8483A, 0xF0E0C8, 0xFFFFFF),     # Zwamset: a mushroom cap
    "galmcape": (0x7A5AC8, 0xB8A0FF, 0xE8E0FF),       # sound rings
    "buitcape": (0x3A2A4A, 0x6A4A2A, 0xFFD24A),       # Mika-buit: dark with gold coins
    "wortelcape": (0x6A4A2A, 0x8A6A3A, 0x6ABA4A),     # roots with a green edge
    "schemercape": (0x2A2A5A, 0x4A3A7A, 0xFFF4A0),    # dusk with stars
    # zone 4 / zone 5 (their slices made the items; the look lives here)
    "lampioncape": (0xFF9A4A, 0xFFC870, 0xC84A3A),
    "vliegercape": (0x4A8ADC, 0xFFD24A, 0xFFFFFF),
    "wolkwachtcape": (0xF4F8FF, 0x9ACCF0, 0xC8C8D8),
    # any other cape
    "standaard": (0xC86A9A, 0xE8A0C8, 0xFFFFFF),
}

HANDSCHOENEN = {
    "pluiswantjes": (0xFAF6FF, 0xFFB8DC, 0xFFFFFF),
    "kloddergrijpers": (0x6ACA5A, 0x3A8A3A, 0xB8F0A0),
    "ijswantjes": (0xC8ECFF, 0xFFFFFF, 0x8AC8F0),
    "kristalhandschoenen": (0x8AD8F0, 0xE0F8FF, 0xFFFFFF),
    "bloesemhandjes": (0xFFC8E0, 0xFFFFFF, 0xFF7FB8),
    "schorshandschoenen": (0x6A4A2A, 0x8A6A40, 0x4A3018),
    "schelpenwanten": (0xFFD8B8, 0xF0B890, 0xFFFFFF),
    "koraalwantjes": (0xF07A5A, 0xFFB09A, 0xFFE0D0),
    "wolkwachthandschoenen": (0xE8EEF8, 0x9ACCF0, 0xB0B8C8),
    "standaard": (0xD8C8B8, 0xB89A7A, 0xFFFFFF),
}

ARMBANDEN = {
    "pluisarmband": (0xFFFFFF, 0xF0E8FA, 0xFFB8DC),
    "rozenarmbandje": (0xE8C860, 0xFFD24A, 0xE83A6A),
    "kristalarmband": (0xC8D8E8, 0xA8B8C8, 0x8AE8FF),
    "honingarmband": (0xE8B030, 0xFFD24A, 0xFFF0A0),
    "harsarmband": (0xC87A2A, 0xE8A04A, 0xFFD890),
    "zeeglasarmband": (0xD8C8A0, 0xC0B088, 0x6ADCC0),
    "stormarmband": (0x6A7A9A, 0x8A9AB8, 0xFFE04A),
}

GOUD = (0xE8C050, 0xFFE490, 0xB08A30)


def _rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, 255)


def _mix(a, b, t):
    ra, ga, ba = (a >> 16) & 255, (a >> 8) & 255, a & 255
    rb, gb, bb = (b >> 16) & 255, (b >> 8) & 255, b & 255
    return (int(ra + (rb - ra) * t) << 16) | (int(ga + (gb - ga) * t) << 8) | int(ba + (bb - ba) * t)


# ---------------------------------------------------------------------------------------------------------------------
# geometry (model px of the body part -> item px)
# ---------------------------------------------------------------------------------------------------------------------
def _el(mx0, mx1, my0, my1, mz0, mz1, uv, tint=False):
    """A cuboid given in model px; every face shows texture region uv; tintindex 0 when tint."""
    fr = [8 - mx1, -my1, mz0 + 8]
    to = [8 - mx0, -my0, mz1 + 8]
    faces = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        faces[f] = {"uv": list(uv), "texture": "#t"}
        if tint:
            faces[f]["tintindex"] = 0
    return {"from": [round(v, 3) for v in fr], "to": [round(v, 3) for v in to], "faces": faces}


# texture regions
GLOVE, CUFF, KNOKKEL = [0, 0, 8, 8], [8, 0, 16, 4], [8, 4, 16, 8]
BAND, STEEN = [0, 0, 16, 4], [0, 4, 4, 8]
METAAL, EDEL = [0, 0, 8, 8], [8, 0, 16, 8]


def handschoen_elementen():
    """Around the lower arm (arm px y -2..10, 4x4; centred): the hand + a cuff, 0.5 px proud of the sleeve layer."""
    return [
        _el(-2.55, 2.55, 6.6, 10.55, -2.55, 2.55, GLOVE),          # the glove
        _el(-2.85, 2.85, 5.4, 6.8, -2.85, 2.85, CUFF),             # the cuff
        _el(-1.6, 1.6, 8.4, 9.2, -2.75, -2.5, KNOKKEL),            # knuckle stripe on the back of the hand
    ]


def armband_elementen():
    """A band just above the glove cuff, with one stone on the outside."""
    return [
        _el(-2.45, 2.45, 4.0, 5.2, -2.45, 2.45, BAND),
        _el(-0.6, 0.6, 4.0, 5.2, -2.85, -2.4, STEEN),
    ]


def ring_elementen(tint):
    """A thin band round the hand + one stone on the back of the hand (tinted in the generic model)."""
    return [
        _el(-2.7, 2.7, 8.6, 9.3, -2.7, 2.7, METAAL),
        _el(-0.55, 0.55, 8.4, 9.5, -3.05, -2.65, EDEL, tint),
    ]


def ketting_elementen():
    """On the chest (body px x -4..4, y 0..12, front z = -2): a V of chain from the collar + a pendant."""
    z0, z1 = -2.6, -2.3
    el = []
    for i, (x, y) in enumerate([(3.4, 0.0), (2.6, 0.9), (1.8, 1.8), (1.0, 2.6)]):
        el.append(_el(x - 0.8, x, y, y + 0.9, z0, z1, METAAL))
        el.append(_el(-x, -x + 0.8, y, y + 0.9, z0, z1, METAAL))
    el.append(_el(-0.4, 0.4, 3.3, 3.9, z0, z1, METAAL))
    el.append(_el(-0.9, 0.9, 3.9, 5.5, -2.9, -2.3, EDEL, True))   # the pendant
    return el


def oorbel_elementen():
    """Both ears (head px x -4..4, y -8..0, z -4..4): a short drop under each side of the head, 0.6 px proud of the hat layer."""
    el = []
    for s in (-1, 1):
        x0, x1 = (-4.85, -4.4) if s < 0 else (4.4, 4.85)
        el.append(_el(x0, x1, -3.4, -2.4, -0.25, 0.25, METAAL))
        el.append(_el(x0 - 0.15 if s < 0 else x0, x1 if s < 0 else x1 + 0.15, -2.4, -1.3, -0.45, 0.45, EDEL, True))
    return el


# ---------------------------------------------------------------------------------------------------------------------
# textures
# ---------------------------------------------------------------------------------------------------------------------
def tex_kaap(naam, kleuren):
    """64x32, vanilla cape layout: the outside at (1,1)-(11,17), the inside at (12,1)-(22,17), edges around."""
    hoofd, tweede, accent = kleuren
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 22, 17], fill=_rgb(_mix(hoofd, 0x000000, 0.18)))
    d.rectangle([12, 1, 21, 16], fill=_rgb(tweede))   # inside
    # the outside: a soft top-to-bottom shade
    for y in range(1, 17):
        for x in range(1, 11):
            im.putpixel((x, y), _rgb(_mix(hoofd, 0x000000, 0.12 * y / 16)))
    rnd = random.Random("cape_" + naam)
    px = lambda x, y, c: im.putpixel((x, y), _rgb(c))
    if naam == "knuffelcapeje":
        for (x, y) in [(4, 6), (5, 6), (7, 6), (8, 6), (4, 7), (5, 7), (6, 7), (7, 7), (8, 7), (5, 8), (6, 8), (7, 8), (6, 9)]:
            px(x - 0, y, 0xFF4FA8)
        for x in range(1, 11):
            px(x, 16, accent)
    elif naam == "sporencape":
        for (cx, cy, r) in [(3, 4, 1), (8, 6, 1), (5, 10, 1), (8, 13, 0), (3, 14, 0), (6, 2, 0)]:
            for dx in range(-r, r + 1):
                for dy in range(-r, r + 1):
                    if abs(dx) + abs(dy) <= r:
                        px(cx + dx, cy + dy, accent)
        for x in range(1, 11):
            px(x, 16, tweede)
            px(x, 15, tweede if x % 2 else hoofd)
    elif naam == "galmcape":
        for y in range(1, 17):
            for x in range(1, 11):
                r = ((x - 5.5) ** 2 + (y - 4) ** 2) ** 0.5
                if int(r) % 4 == 0:
                    px(x, y, tweede)
                if int(r) % 8 == 0:
                    px(x, y, accent)
    elif naam == "buitcape":
        for (x, y) in [(3, 5), (7, 4), (5, 9), (8, 11), (3, 13), (6, 14)]:
            px(x, y, accent)
            px(x + 1, y, _mix(accent, 0x000000, 0.25))
        for x in range(1, 11):
            px(x, 16, accent)
        for y in range(1, 17):
            px(1, y, tweede)
            px(10, y, tweede)
    elif naam == "wortelcape":
        for y in range(1, 17):
            for x in range(1, 11):
                if (x * 3 + y + rnd.randint(0, 1)) % 7 == 0:
                    px(x, y, tweede)
        for x in range(1, 11):
            px(x, 16, accent)
            if rnd.random() < 0.5:
                px(x, 15, accent)
    elif naam == "schemercape":
        for y in range(1, 17):
            for x in range(1, 11):
                im.putpixel((x, y), _rgb(_mix(hoofd, tweede, y / 16)))
        for (x, y) in [(3, 3), (7, 5), (4, 9), (8, 11), (5, 14), (2, 12), (8, 2)]:
            px(x, y, accent)
        px(6, 7, 0xFFFFFF)
    elif naam == "lampioncape":
        for y in range(1, 17):
            for x in range(1, 11):
                if (y // 3) % 2:
                    px(x, y, tweede)
        for y in range(1, 17):
            px(1, y, accent)
            px(10, y, accent)
    elif naam == "vliegercape":
        for y in range(1, 17):
            for x in range(1, 11):
                if abs(x - 5.5) + abs(y - 8.5) < 5:
                    px(x, y, tweede)
        for y in range(2, 16):
            px(5, y, accent)
        for x in range(2, 10):
            px(x, 8, accent)
    elif naam == "wolkwachtcape":
        for x in range(1, 11):
            px(x, 1, accent)
            px(x, 16, accent)
        for (cx, cy) in [(4, 6), (7, 10)]:
            for dx in range(-2, 3):
                px(cx + dx, cy, tweede)
            for dx in range(-1, 2):
                px(cx + dx, cy - 1, tweede)
    else:  # standaard
        for x in range(1, 11):
            px(x, 16, accent)
            px(x, 1, tweede)
    # the edges (top/bottom/sides) follow the outside colour
    return im


def tex_handschoen(naam, kleuren):
    hoofd, tweede, accent = kleuren
    im = Image.new("RGBA", (16, 16), _rgb(hoofd))
    rnd = random.Random("handschoen_" + naam)
    for y in range(8):
        for x in range(8):
            c = hoofd
            if naam in ("pluiswantjes", "wolkwachthandschoenen") and rnd.random() < 0.25:
                c = _mix(hoofd, tweede, 0.5)
            elif naam == "kloddergrijpers" and (x + y * 2) % 5 == 0:
                c = accent
            elif naam in ("ijswantjes", "kristalhandschoenen") and (x - y) % 4 == 0:
                c = accent
            elif naam == "bloesemhandjes" and (x, y) in ((2, 2), (5, 4), (3, 6), (6, 1)):
                c = accent
            elif naam == "schorshandschoenen" and (x % 3 == 0 or rnd.random() < 0.15):
                c = accent
            elif naam == "schelpenwanten" and y % 3 == 0:
                c = tweede
            elif naam == "koraalwantjes" and rnd.random() < 0.2:
                c = accent
            elif y == 7:
                c = _mix(hoofd, 0x000000, 0.15)
            im.putpixel((x, y), _rgb(c))
    for y in range(0, 4):           # cuff
        for x in range(8, 16):
            im.putpixel((x, y), _rgb(tweede if y in (1, 2) else _mix(tweede, 0x000000, 0.2)))
    for y in range(4, 8):           # knuckles
        for x in range(8, 16):
            im.putpixel((x, y), _rgb(accent if (x + y) % 2 else tweede))
    return im


def tex_armband(naam, kleuren):
    hoofd, tweede, steen = kleuren
    im = Image.new("RGBA", (16, 16), _rgb(hoofd))
    for y in range(4):
        for x in range(16):
            c = hoofd if y in (1, 2) else tweede
            if naam == "rozenarmbandje" and x % 4 == 0 and y in (1, 2):
                c = steen
            if naam == "stormarmband" and (x + y) % 5 == 0:
                c = steen
            im.putpixel((x, y), _rgb(c))
    for y in range(4, 8):
        for x in range(4):
            im.putpixel((x, y), _rgb(0xFFFFFF if (x, y) == (1, 5) else steen))
    return im


def tex_sieraad():
    """The generic jewellery: gold (0..8) and a white gem (8..16) that the dye tint colours."""
    im = Image.new("RGBA", (16, 16), _rgb(GOUD[0]))
    for y in range(8):
        for x in range(8):
            im.putpixel((x, y), _rgb(GOUD[1] if (x + y) % 5 == 0 else GOUD[0] if y < 6 else GOUD[2]))
        for x in range(8, 16):
            c = 0xFFFFFF if (x - 8, y) in ((1, 1), (2, 1), (1, 2)) else 0xE8E8E8 if (x + y) % 3 else 0xC8C8C8
            im.putpixel((x, y), _rgb(c))
    return im


# ---------------------------------------------------------------------------------------------------------------------
# writing
# ---------------------------------------------------------------------------------------------------------------------
def _schrijf_json(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _model(naam, elementen, tex, root, tint=False):
    t = f"guhs:item/vr_u_zichtbaar/{tex}"
    _schrijf_json(os.path.join(root, "models", "item", "vr_u_zichtbaar", f"{naam}.json"),
                  {"textures": {"t": t, "particle": t}, "elements": elementen})
    m = {"type": "minecraft:model", "model": f"guhs:item/vr_u_zichtbaar/{naam}"}
    if tint:
        m["tints"] = [{"type": "minecraft:dye", "default": -1}]
    _schrijf_json(os.path.join(root, "items", f"vr_u_zichtbaar_{naam}.json"), {"model": m})


def assets(root=A):
    for n in HANDSCHOENEN:
        _model(f"handschoen_{n}", handschoen_elementen(), f"handschoen_{n}", root)
    for n in ARMBANDEN:
        _model(f"armband_{n}", armband_elementen(), f"armband_{n}", root)
    _model("ring_standaard", ring_elementen(True), "sieraad", root, tint=True)
    _model("ketting_standaard", ketting_elementen(), "sieraad", root, tint=True)
    _model("oorbel_standaard", oorbel_elementen(), "sieraad", root, tint=True)
    # a generic armband: gold band, tinted stone
    _model("armband_standaard", [_el(-2.45, 2.45, 4.0, 5.2, -2.45, 2.45, METAAL), _el(-0.6, 0.6, 4.0, 5.2, -2.85, -2.4, EDEL, True)],
           "sieraad", root, tint=True)
    if Image is None:
        print("vadsrijk_u_zichtbaar: no PIL, textures skipped")
        return
    td = os.path.join(root, "textures", "item", "vr_u_zichtbaar")
    os.makedirs(td, exist_ok=True)
    for n, k in HANDSCHOENEN.items():
        tex_handschoen(n, k).save(os.path.join(td, f"handschoen_{n}.png"))
    for n, k in ARMBANDEN.items():
        tex_armband(n, k).save(os.path.join(td, f"armband_{n}.png"))
    tex_sieraad().save(os.path.join(td, "sieraad.png"))
    kd = os.path.join(root, "textures", "vadsrijk", "zichtbaar")
    os.makedirs(kd, exist_ok=True)
    for n, k in KAPEN.items():
        tex_kaap(n, k).save(os.path.join(kd, f"cape_{n}.png"))


def build(h):
    assets()


if __name__ == "__main__":
    assets()
    print(f"vadsrijk_u_zichtbaar: {len(KAPEN)} capes, {len(HANDSCHOENEN)} gloves, {len(ARMBANDEN)} armbands, 4 generic jewellery models")
