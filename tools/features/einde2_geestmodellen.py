"""
einde2 (the Guheinde expansion), slice geestmodellen: the creatures of the Geestenwereld (DESIGN_EINDE2 5) as entity types of
their own, and two last characters as GuhNpcEntity kinds (the Theegeest, the Bestelguh of tech tier 5). Phase A only gives them
a body, looks, animations, a name and two lines; no story logic. Java: nl.juiced.guhs.feature.einde2.geestwezens
(E2GeestWezensFeature; the tables GeestWezens and GeestKinds).

  einde2_geestmodellen_wezens   the seven creatures: models, textures (+ glow masks), animations, check, the preview's input
  einde2_geestmodellen_kinds    the two kinds (drawn with cast_team's toolkit), their animation files, check
  einde2_geestmodellen_tekst    every Dutch text
  this module                   names and small talk, the spawn eggs, the (empty) loot tables, the game test room, the
                                self-check, and the offline preview:  python tools/features/einde2_geestmodellen.py <out dir> [names]
"""
import json
import os
import re
import sys

try:
    from features import einde2_geestmodellen_kinds as kinds
    from features import einde2_geestmodellen_tekst as tekst
    from features import einde2_geestmodellen_wezens as wezens
    from features import einde2_kit as kit
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import einde2_geestmodellen_kinds as kinds
    from features import einde2_geestmodellen_tekst as tekst
    from features import einde2_geestmodellen_wezens as wezens
    from features import einde2_kit as kit

# the game test room (Java: E2GeestWezensGameTests): a plain floor of 25 x 25 with room above it
TEST_VLOER = "einde2_geestmodellen_test_vloer"
VLOER = 25


def texts(h):
    for key, text in tekst.texts().items():
        kit.lang(h, key, text)


def eieren(h):
    """The spawn eggs and the (empty) loot tables of the creatures: no spirit ever drops anything."""
    for id, (naam, basis, vlek) in tekst.WEZENS.items():
        kit.spawn_ei(h, id, f"{naam}-spawnei", basis, vlek)
        h.w(f"{h.D}/loot_table/entities/{id}.json", {"type": "minecraft:entity", "pools": []})


def test_kamer(h):
    s = h.Structure((VLOER, 12, VLOER))
    for x in range(VLOER):
        for z in range(VLOER):
            s.set(x, 0, z, ("minecraft:purple_concrete", "minecraft:white_concrete")[(x + z) % 2])
    s.save(TEST_VLOER)


def build(h):
    wezens.build(h)
    kinds.build(h)
    texts(h)
    eieren(h)
    test_kamer(h)
    selfcheck(h)


def _model_check(problems):
    def laad(pad):
        try:
            return json.load(open(pad, encoding="utf-8"))
        except OSError as e:
            problems.append(str(e))
            return None

    def model_check(naam, geo_pad, anim_pad, wil, basisbotten=()):
        g, a = laad(geo_pad), laad(anim_pad)
        if g is None or a is None:
            return set()
        botten = [b["name"] for b in g["minecraft:geometry"][0]["bones"]]
        namen = set(botten)
        if len(namen) != len(botten):
            problems.append(f"{naam}: a bone name twice")
        for b in g["minecraft:geometry"][0]["bones"]:
            if b.get("parent") and b["parent"] not in namen:
                problems.append(f"{naam}: bone {b['name']} has a missing parent {b['parent']}")
        for must in basisbotten:
            if must not in namen:
                problems.append(f"{naam}: the base bone {must} is gone")
        anims = a["animations"]
        if not anims:
            problems.append(f"{naam}: no animations at all (GeckoLib refuses that)")
        for an in wil:
            if an not in anims:
                problems.append(f"{naam}: no animation {an}")
        for an, inhoud in anims.items():
            for bn in inhoud.get("bones", {}):
                if bn not in namen:
                    problems.append(f"{naam}: animation {an} moves a missing bone {bn}")
        return namen
    return model_check


def selfcheck(h):
    problems = []
    problems += wezens.check(h, _model_check(problems))
    problems += kinds.check(h)
    for id in wezens.WEZENS:
        for pad in (f"{h.A}/models/item/{id}_spawn_egg.json", f"{h.D}/loot_table/entities/{id}.json"):
            if not os.path.exists(pad):
                problems.append(f"missing {pad}")
        for key in (f"entity.guhs.{id}", f"item.guhs.{id}_spawn_egg", f"{tekst.P}{id}.0", f"{tekst.P}{id}.1"):
            if key not in h.NL:
                problems.append(f"no text {key}")
    for kind in kinds.KINDS:
        for key in (f"entity.guhs.guh_npc.{kind}", f"{tekst.P}{kind}.0", f"{tekst.P}{kind}.1"):
            if key not in h.NL:
                problems.append(f"no text {key}")
    if set(tekst.WEZENS) != set(wezens.WEZENS) or set(tekst.KINDS) != set(kinds.KINDS):
        problems.append("the names (tekst) and the tables (wezens.WEZENS, kinds.KINDS) list other things")
    pad = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "geestwezens", "GeestWezens.java")
    java = open(pad, encoding="utf-8").read() if os.path.exists(pad) else ""
    for id in wezens.WEZENS:
        m = re.search(r"^\s*" + id.upper() + r"\((.*?)\)[,;]\s*$", java, re.M)
        wil = wezens.java_rij(id)
        if not m or " ".join(m.group(1).split()) != wil:
            problems.append(f"GeestWezens.{id.upper()} does not say ({wil})")
    fang = os.path.join(os.path.dirname(pad), "FangEntity.java")
    zx, zy, zz = wezens.FANG_ZADEL
    if not os.path.exists(fang) or f"new Vec3({zx:.1f}, {zy:.1f} / 16.0, {-zz:.1f} / 16.0)" not in open(fang, encoding="utf-8").read():
        problems.append(f"FangEntity.ZADEL is not the bone zadel {wezens.FANG_ZADEL}")
    if not os.path.exists(f"{h.D}/structure/{TEST_VLOER}.nbt"):
        problems.append(f"missing the test room {TEST_VLOER}")
    if problems:
        raise SystemExit("einde2_geestmodellen self-check failed:\n  " + "\n  ".join(problems))


# =====================================================================================================================
# the offline preview (front, three-quarter, behind, and a pose of each named animation)
# =====================================================================================================================
class _Vang:
    """A stand-in for the generator namespace: it reads the base files of the repo and keeps what is written in memory."""

    def __init__(self):
        sys.path.insert(0, "tools")
        import make_v2
        self.A, self.TEX, self.D = make_v2.A, make_v2.TEX, make_v2.D
        self.recolour, self.pinkish = make_v2.recolour, make_v2.pinkish
        self.json, self.png = {}, {}

    def w(self, pad, obj):
        self.json[os.path.basename(str(pad))] = obj

    def save(self, img, *pad):
        self.png[pad[-1]] = img


def _strook(tegels, maat):
    from PIL import Image
    strook = Image.new("RGBA", (maat * len(tegels), maat), (60, 56, 78, 255))
    for i, t in enumerate(tegels):
        strook.alpha_composite(t, (i * maat, 0))
    return strook


def preview(out, alleen=None):
    """Renders into out/: <name>.png (front, three-quarter, side-behind, top) and <name>_poses.png (the named animations
    halfway). Glowing pixels are drawn from the glow mask on top."""
    import numpy as np
    from features import einde2_cast_team_kit as ck
    os.makedirs(out, exist_ok=True)
    maat, hoeken = 300, ((0, -6), (35, -16), (150, -16), (0, -60))

    def tegels(geo, tex, pose, hide, hk):
        g = json.loads(json.dumps(geo["minecraft:geometry"][0]))
        weg = set(hide)
        while True:
            meer = {b["name"] for b in g["bones"] if b.get("parent") in weg} - weg
            if not meer:
                break
            weg |= meer
        g["bones"] = [b for b in g["bones"] if b["name"] not in weg]
        q = ck.quads(g, tex.astype(np.float32), pose)
        return [ck.render(q, yaw, pitch, maat) for yaw, pitch in hk]

    def op(a, t):
        vast = {"bones": {b: {k: (v if isinstance(v, dict) else {"0.0": v}) for k, v in kan.items()} for b, kan in a["bones"].items()}}
        return ck.op(vast, t)

    namen = []
    for naam, geo, img, gloed, hide, anims in wezens.voorbeelden(alleen):
        tex = img.copy()
        m = gloed[..., 3] > 0
        tex[m, :3] = np.maximum(tex[m, :3], gloed[m, :3])
        _strook(tegels(geo, tex, op(anims["idle"], 0.0), hide, hoeken), maat).save(os.path.join(out, f"{naam}.png"))
        poses = [(n, a) for n, a in anims.items() if n != "idle"]
        _strook([tegels(geo, tex, op(a, a["animation_length"] * 0.5), hide, ((35, -16),))[0] for n, a in poses], maat).save(
            os.path.join(out, f"{naam}_poses.png"))
        namen.append(naam)
    vang = _Vang()
    for kind, teken in kinds.TEKEN.items():
        if alleen and kind not in alleen:
            continue
        teken(vang, kind)
        geo = vang.json[f"guh_npc_{kind}.geo.json"]
        from PIL import Image
        tex = np.asarray(vang.png[f"npc_{kind}.png"].convert("RGBA")).copy()
        anims = {n.split(".")[-1]: a for n, a in kinds.bestand(kind)["animations"].items()}
        _strook(tegels(geo, tex, op(anims["idle"], 0.0), (), hoeken), maat).save(os.path.join(out, f"{kind}.png"))
        poses = [(n, a) for n, a in anims.items() if n != "idle"]
        _strook([tegels(geo, tex, op(a, a["animation_length"] * 0.5), (), ((35, -16),))[0] for n, a in poses], maat).save(
            os.path.join(out, f"{kind}_poses.png"))
        namen.append(kind)
        del Image
    return namen


if __name__ == "__main__":
    print("\n".join(preview(sys.argv[1] if len(sys.argv) > 1 else ".", sys.argv[2:] or None)))
