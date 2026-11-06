"""Renderkit voor de 1.7.0 "Onder de Wol"-concepten.

Bovenop vibekit (base-ontwerpen/vibes/_bron): zelfde Scene/render, maar met de 1.6.4-assets uit de bronboom
en een concept-overlay met nog niet bestaande blokken (gemaakt door concept_blokken.py).
Renders komen in mijnen-ontwerp/<naam>.png.
"""
import os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)
import vibekit as V  # noqa: E402

CONCEPT = None
V.JARS = [
          # [zone 4 kaart] the asset zips stay outside the repo (MineScapeStory/renders-*), only read for block shapes
          os.path.join(os.path.abspath(os.path.join(HERE, "..", "..", "..", "..", "..", "..")), "renders-lampionkust", "_bron", "jars", "guhs-assets-1.10-kint.zip"),
          os.path.join(os.path.abspath(os.path.join(HERE, "..", "..", "..", "..", "..", "..")), "renders-erfenis", "_bron", "jars", "guhs-assets-1.8.1.zip"),
          os.path.join(os.environ.get("APPDATA", ""),
                       "PrismLauncher/libraries/com/mojang/minecraft/26.1.2/minecraft-26.1.2-client.jar")]

V.EMISSIVE |= {"guhs:c_glitterrank", "guhs:c_glitterrank_bes", "guhs:c_glinsterpaddenstoel", "guhs:c_vahoegiet_erts",
               "guhs:guh_kristal_cluster", "guhs:guh_kristal_blok", "guhs:disco_bal", "guhs:disco_tegel_roze",
               "guhs:disco_tegel_blauw", "guhs:disco_tegel_geel", "guhs:disco_tegel_groen", "guhs:sterrenplafond",
               "guhs:klusjes_guhlampje", "guhs:c_bodem"}

Scene, render, stairs, slab, has_block, state_props = V.Scene, V.render, V.stairs, V.slab, V.has_block, V.state_props

# Lagen (ronde 5): hoogte in een scene met wereld-y = scene-y
LAAG = [(64, "minecraft:pink_wool"), (45, "c_viltsteen"), (25, "knuffelsteen"), (5, "guh_kristalsteen"),
        (0, "c_bodem")]


def laagblok(y):
    for (y0, b) in LAAG:
        if y >= y0:
            return b
    return "c_bodem"


ERTS = {  # erts -> variant per laag (ronde 8)
    "vads": {"c_viltsteen": "c_vadserts_vilt", "knuffelsteen": "c_vadserts_knuffel",
             "guh_kristalsteen": "c_vadserts_kristal"},
    "knabbel": {"c_viltsteen": "c_knabbelerts_vilt", "knuffelsteen": "c_knabbelerts_knuffel",
                "guh_kristalsteen": "c_knabbelerts_kristal"},
    "klei": {"c_viltsteen": "c_guhklei", "knuffelsteen": "c_guhklei", "guh_kristalsteen": "c_guhklei"},
    "pigment": {"c_viltsteen": "c_pigmenterts_vilt", "knuffelsteen": "c_pigmenterts_knuffel",
                "guh_kristalsteen": "c_pigmenterts_knuffel"},
    "vahoegiet": {"guh_kristalsteen": "c_vahoegiet_erts"},
}


def _textuur_van(r, n):
    """Zoekt via blockstate -> model (-> parents) een texture-pad voor een blok."""
    ns, p = n.split("[")[0].split(":")
    bs = r.json(f"{ns}:{p}", "blockstates")
    model = None
    if bs:
        if bs.get("variants"):
            v = next(iter(bs["variants"].values()))
            model = (v[0] if isinstance(v, list) else v).get("model")
        elif bs.get("multipart"):
            a = bs["multipart"][0]["apply"]
            model = (a[0] if isinstance(a, list) else a).get("model")
    for _ in range(6):
        if not model:
            break
        mns, mp = (model.split(":") + [""])[:2] if ":" in model else ("minecraft", model)
        m = r.json(f"{mns}:{mp}", "models")
        if not m:
            break
        tx = m.get("textures", {})
        for k in ("all", "side", "front", "top", "cross", "wool", "texture", "particle", "end"):
            t = tx.get(k)
            if t and not t.startswith("#"):
                return t
        for t in tx.values():
            if not t.startswith("#"):
                return t
        model = m.get("parent")
    return None


def _swatch(blok):
    import io
    from PIL import Image
    n = blok if ":" in blok else "guhs:" + blok
    ns, p = n.split("[")[0].split(":")
    r = V.res()[0]
    paden = [f"assets/{ns}/textures/block/{k}.png" for k in (p, p + "_still", p + "_top")]
    try:
        t = _textuur_van(r, n)
        if t:
            tns, tp = t.split(":") if ":" in t else ("minecraft", t)
            paden.insert(0, f"assets/{tns}/textures/{tp}.png")
    except Exception:
        pass
    for pad in paden:
        data = r.read(pad)
        if data:
            im = Image.open(io.BytesIO(data)).convert("RGBA")
            return im.crop((0, 0, 16, 16)).resize((28, 28), Image.NEAREST)
    return Image.new("RGBA", (28, 28), (200, 200, 200, 255))


def legenda(pad, regels, titel=None, breedte=300):
    """Plakt rechts een legenda-paneel aan een render: regels = [(naam, uitleg, blok) | None voor witregel]."""
    from PIL import Image, ImageDraw, ImageFont
    im = Image.open(pad).convert("RGB")
    try:
        f = ImageFont.truetype("segoeui.ttf", 16)
        fb = ImageFont.truetype("segoeuib.ttf", 19)
        fk = ImageFont.truetype("segoeui.ttf", 13)
    except OSError:
        f = fb = fk = ImageFont.load_default()
    nieuw = Image.new("RGB", (im.width + breedte, max(im.height, 60 + 36 * len(regels))), (252, 244, 248))
    nieuw.paste(im, (0, 0))
    d = ImageDraw.Draw(nieuw)
    y = 18
    if titel:
        d.text((im.width + 14, y), titel, font=fb, fill=(110, 40, 90))
        y += 38
    for r in regels:
        if r is None:
            y += 12
            continue
        naam, uitleg, blok = r
        if blok:
            nieuw.paste(_swatch(blok), (im.width + 14, y), _swatch(blok))
        d.text((im.width + 50, y), naam, font=f, fill=(70, 40, 60))
        if uitleg:
            d.text((im.width + 50, y + 17), uitleg, font=fk, fill=(140, 110, 130))
        y += 36
    nieuw.save(pad)


# Het guh-model bevat sinds 1.5/1.6 de botten van alle varianten (guhshi, kristalguh, duinguh ...). De oude
# GEO_SKIP-lijst kent die niet, waardoor guhs als guhsi's renderden. Hier tekenen we alleen de basis-guh.
GUH_BASIS = {"root", "body", "tail", "head", "ear_left", "ear_right", "leg_back_left", "leg_back_right",
             "leg_front_left", "leg_front_right", "foot_left", "foot_right", "arm_left", "arm_right"}
_geo_orig = V.E.geo_quads


def _geo_basis(res, geo_path, tex_path, Mt, tint=(1, 1, 1), skip=V.E.GEO_SKIP):
    if geo_path.endswith(("/guh.geo.json", "/guh_sitting.geo.json")):
        import json
        bones = json.loads(res.read(geo_path))["minecraft:geometry"][0]["bones"]
        skip = tuple(b["name"] for b in bones if b["name"] not in GUH_BASIS)
    return _geo_orig(res, geo_path, tex_path, Mt, tint, skip)


V.E.geo_quads = _geo_basis
