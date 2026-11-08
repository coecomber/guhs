"""
U-SITE: 3/4 renders of every Vadsrijk creature and boss for guhs.nl/vadsrijk (GeckoLib model + texture + glow, idle pose).

Uses t-models' offline previewer tools/vadsrijk/model_voorbeeld.py (which needs the render pipeline outside the repo:
renders-mounts/_bron + base-ontwerpen/_bron). The output is committed in docs/wiki/vadsrijk_renders/<id>.webp (like the wiki's
docs/wiki/img), so CI never needs the renderer; the site build only copies them. Re-run after a model change:

    python tools/wiki_site/vadsrijk/renders.py            (only what changed: a hash of geo + texture + glow per id)
    python tools/wiki_site/vadsrijk/renders.py --alles    (everything again)
"""
import glob
import hashlib
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(HERE)))
ZONEDIR = {"zone1": ["wezens", "bazen", "tmodels"], "zone2": ["zone2", "z2bazen"], "zone3": ["zone3", "z3bazen", "z3imker"],
           "zone4": ["zone4", "z4bazen"], "zone5": ["zone5"], "zone6": ["zone6", "z6raid"]}


# kort -> (geo below geckolib/models/entity/vadsrijk, texture below textures/vadsrijk) where the names differ
OVERRIDE = {"klodderreus": ("tmodels/grauwe_klodderreus", "tmodels/grauwe_klodderreus"), "vahoegnir": ("wezens/draak", "z3bazen/vahoegnir_0")}
# U-SITE round 3: entities whose renderer borrows another model: kort -> (geo, texture, animation) relative to assets/guhs
GELEEND = {"mika_spion": ("geckolib/models/entity/mika.geo.json", "textures/vadsrijk/bazen/mika_spion.png", "geckolib/animations/entity/guh.animation.json"),
           "kloddertje": ("geckolib/models/entity/vadsrijk/wezens/klodder.geo.json", "textures/vadsrijk/wezens/kloddertje.png",
                          "geckolib/animations/entity/vadsrijk/wezens/klodder.animation.json"),
           "honingbij": ("geckolib/models/entity/guh_bee.geo.json", "textures/entity/guh_bee.png", "geckolib/animations/entity/guh_bee.animation.json")}


def _alle(root, sub, ext):
    out = {}
    for f in glob.glob(os.path.join(root, sub, "**", "*" + ext), recursive=True):
        out.setdefault(os.path.basename(f)[:-len(ext)], []).append(f)
    return out


def vind(root, eid, kort, zone):
    """(geo, texture, glow or None, animation or None) for an entity, or None."""
    a = os.path.join(root, "src", "main", "resources", "assets", "guhs")
    if kort in GELEEND:
        geo, tex, an = (os.path.join(a, *x.split("/")) for x in GELEEND[kort])
        if os.path.exists(geo) and os.path.exists(tex):
            gl = next((x for x in (tex[:-4] + "_gloed.png", tex[:-4] + "_glowmask.png") if os.path.exists(x)), None)
            return geo, tex, gl, an if os.path.exists(an) else None
    if kort in OVERRIDE:
        g, tx = OVERRIDE[kort]
        geo = os.path.join(a, "geckolib", "models", "entity", "vadsrijk", *g.split("/")) + ".geo.json"
        tex = os.path.join(a, "textures", "vadsrijk", *tx.split("/")) + ".png"
        if os.path.exists(geo) and os.path.exists(tex):
            gl = next((x for x in (tex[:-4] + "_gloed.png", tex[:-4] + "_glowmask.png") if os.path.exists(x)), None)
            an = os.path.join(a, "geckolib", "animations", "entity", "vadsrijk", *g.split("/")) + ".animation.json"
            return geo, tex, gl, an if os.path.exists(an) else None
    geos = _alle(a, os.path.join("geckolib", "models", "entity"), ".geo.json")
    texs = _alle(a, "textures", ".png")
    dirs = ZONEDIR.get(zone, [])

    def beste(lijst, voorkeur_vr=True):
        if not lijst:
            return None
        def score(f):
            rel = f.replace("\\", "/")
            s = 0
            if "/vadsrijk/" in rel:
                s -= 2
            if any(f"/vadsrijk/{d}/" in rel for d in dirs):
                s -= 4
            if "/item/" in rel or "/huisdier" in rel:
                s += 10
            return s
        return sorted(lijst, key=score)[0]
    geo = beste(geos.get(eid)) or beste(geos.get(kort))
    if not geo:
        return None
    tex = beste(texs.get(eid)) or beste(texs.get(kort))
    if not tex:
        return None
    gl = None
    for g in (tex[:-4] + "_gloed.png", tex[:-4] + "_glowmask.png"):
        if os.path.exists(g):
            gl = g
            break
    rel = os.path.relpath(geo, os.path.join(a, "geckolib", "models"))
    an = os.path.join(a, "geckolib", "animations", rel[:-len(".geo.json")] + ".animation.json")
    return geo, tex, gl, an if os.path.exists(an) else None


def _hash(paden):
    h = hashlib.sha1()
    for p in paden:
        if p and os.path.exists(p):
            h.update(open(p, "rb").read())
    return h.hexdigest()[:16]


def maak(root, doelen, uit, alles=False):
    """doelen: [(id, entity id, kort, zone)]. Writes uit/<id>.webp; returns {id: file name} of what exists."""
    sys.path.insert(0, os.path.join(root, "tools", "vadsrijk"))
    os.makedirs(uit, exist_ok=True)
    idx_pad = os.path.join(uit, "index.json")
    try:
        idx = json.load(open(idx_pad, encoding="utf-8"))
    except (OSError, ValueError):
        idx = {}
    mv = None
    gemaakt, mist = 0, []
    for rid, eid, kort, zone in doelen:
        v = vind(root, eid, kort, zone)
        if not v:
            mist.append(rid)
            continue
        h = _hash(v)
        f = os.path.join(uit, rid + ".webp")
        if not alles and idx.get(rid) == h and os.path.exists(f):
            continue
        if mv is None:
            import model_voorbeeld as mv  # noqa: E402  (needs the render pipeline outside the repo)
        geo, tex, gl, an = v
        try:
            im = mv.render(geo, tex, gl, mv.pose(an, "idle", 0) if an else {}, yaw=215, pitch=18, px=320)
        except Exception as e:  # noqa: BLE001
            print("  render failed:", rid, e)
            mist.append(rid)
            continue
        im.thumbnail((640, 640))
        im.save(f, "WEBP", quality=88)
        idx[rid] = h
        gemaakt += 1
    with open(idx_pad, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(idx, fh, indent=1, sort_keys=True)
    print(f"renders -> {uit}: {gemaakt} new, {len(idx)} total; no model/texture for: {', '.join(mist) or '-'}")
    return {rid: rid + ".webp" for rid in idx if os.path.exists(os.path.join(uit, rid + ".webp"))}


def doelen_van(data):
    out = [("wezen_" + w["kort"], w["id"], w["kort"], w["zone"]) for w in data["wezens"]]
    out += [("baas_" + b["id"], b["entity"], b["id"], b["zone"]) for b in data["bazen"]]
    for v in (data.get("raid") or {}).get("verdiepingen", []):   # U-SITE round 3: the raid bosses
        out += [("raid_" + w["id"], w["entity"], w["id"], "zone6") for w in v["bazen"]]
    return out


if __name__ == "__main__":
    sys.path.insert(0, os.path.join(ROOT, "tools"))
    from wiki_site.vadsrijk.bron import Bron
    d = Bron(ROOT).build()
    maak(ROOT, doelen_van(d), os.path.join(ROOT, "docs", "wiki", "vadsrijk_renders"), alles="--alles" in sys.argv)
