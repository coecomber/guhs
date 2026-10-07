"""
einde2, slice cast_schurk - the looks of its cast (CONTRACT_E2 11.2, 11.6): the table, and everything that is written.

  CAST      the twelve Mika Kinds: one row per GuhNpcEntity.Kind, the same table as the Java enum
            nl.juiced.guhs.feature.einde2.castschurk.CastSchurk (check() compares the two by reading the Java file)
  WEZENS    the nine creatures (entity types): the same table as the Java enum Wezens
  build(h)  per Kind: geckolib/models/entity/guh_npc_<kind>.geo.json, textures/entity/npc_<kind>.png (+ _glowmask where the
            row says gloed), and per CHARACTER geckolib/animations/entity/guh_npc_<basis>.animation.json;
            per creature: <id>.geo.json, <id>.animation.json, <id>.png (+ the textures and models of its looks)
  check(h)  the files, the animation names, every animated bone, the mix bones, the Java tables
  preview(out [, names])   offline renders of every Kind, mix set, creature and look (front, three-quarter, behind):
            python tools/features/einde2_cast_schurk_modellen.py <out dir> [kind or id ...]

The Mika's are drawn in einde2_cast_schurk_mikas (kit: einde2_cast_schurk_blad), the creatures in einde2_cast_schurk_wezens.
"""
import json
import os
import re
import sys

import numpy as np
from PIL import Image

try:
    from features import einde2_cast_schurk_mikas as mikas
    from features import einde2_cast_schurk_wezens as wezens
except ImportError:                                   # (run as a script from the project root)
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
    from features import einde2_cast_schurk_mikas as mikas
    from features import einde2_cast_schurk_wezens as wezens

P = "animation.guh_sitting."
# kind: (basis, lijf, mix sets, doorzichtig, gloed, [(animation besides idle, ticks; 0 = a pose that lasts)])
_MIKAI_LANG = [("praat", 24), ("brul", 32), ("vuurstoot", 28), ("troon", 0), ("komeet", 0)]
CAST = {
    "vuurheer_mikai": ("vuurheer_mikai", "lang", 0, False, False, _MIKAI_LANG),
    "vuurheer_mikai_komeet": ("vuurheer_mikai", "lang", 0, False, True, _MIKAI_LANG),
    "vuurheer_mikai_vadsig": ("vuurheer_mikai", "mika", 0, False, False, [("praat", 24), ("smul", 40), ("boer", 24), ("zwaai", 32)]),
    "azula_mika": ("azula_mika", "mika", 0, False, True, [("praat", 24), ("bliksem", 40), ("grijns", 32), ("lok", 48)]),
    "ty_lee_mika": ("ty_lee_mika", "mika", 0, False, False, [("praat", 24), ("radslag", 28), ("prik", 20), ("handstand", 0)]),
    "zhao_mika": ("zhao_mika", "mika", 0, False, False, [("praat", 24), ("opschep", 40), ("woede", 36)]),
    "long_feng_mika": ("long_feng_mika", "mika", 0, False, False, [("praat", 24), ("buig", 48), ("fluister", 40)]),
    "dai_li_mika": ("dai_li_mika", "mika", 0, False, False, [("praat", 24), ("stenen_hand", 24), ("verschijn", 0)]),
    "vuurnatie_mika": ("vuurnatie_mika", "mika", 3, False, False, [("praat", 24), ("wacht", 80), ("duw", 24), ("salueer", 32)]),
    "gevangenis_directeur_mika": ("gevangenis_directeur_mika", "mika", 0, False, False, [("praat", 24), ("tier", 40), ("sleutels", 48)]),
    "mika_kok": ("mika_kok", "mika", 0, False, False, [("praat", 24), ("roer", 40), ("spijt", 48)]),
    "vestingkapitein_mika": ("vestingkapitein_mika", "mika", 0, False, False, [("praat", 24), ("tuur", 60), ("brul", 32)]),
}
MIX = {"vuurnatie_mika": mikas.MIX_VUURNATIE}
WEZENS = wezens.WEZENS


def _kind(h, kind):
    blad, anims = mikas.MAKERS[kind](h)
    return blad, {"format_version": "1.8.0", "animations": mikas.namen(anims)}


def build(h):
    geschreven = set()
    for kind, (basis, _lijf, _mix, _door, gloed, _anims) in CAST.items():
        blad, anims = _kind(h, kind)
        blad.schrijf(gloed=gloed)
        if basis not in geschreven:
            geschreven.add(basis)
            h.w(f"{h.A}/geckolib/animations/entity/guh_npc_{basis}.animation.json", anims)
    wezens.build(h)


def _java(naam):
    pad = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "castschurk", naam)
    return open(pad, encoding="utf-8").read() if os.path.exists(pad) else None


def check(h):
    problems = []
    geo_dir, anim_dir, tex_dir = f"{h.A}/geckolib/models/entity", f"{h.A}/geckolib/animations/entity", f"{h.A}/textures/entity"

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
            if b["name"].startswith(("outfit_", "neck", "pluis", "teckel_")):
                problems.append(f"{naam}: bone name {b['name']} clashes with the guh variant bone prefixes")
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
    from features import einde2_cast_schurk_blad as blad
    for kind, (basis, lijf, mix, _door, gloed, anims) in CAST.items():
        namen = model_check(kind, f"{geo_dir}/guh_npc_{kind}.geo.json", f"{anim_dir}/guh_npc_{basis}.animation.json",
                            [P + "idle"] + [P + n for n, _ in anims], blad.LANG_BOTTEN if lijf == "lang" else blad.MIKA_BOTTEN)
        if not os.path.exists(f"{tex_dir}/npc_{kind}.png"):
            problems.append(f"{kind}: no texture")
        if gloed and not os.path.exists(f"{tex_dir}/npc_{kind}_glowmask.png"):
            problems.append(f"{kind}: no glowmask")
        sets = MIX.get(kind, [])
        if len(sets) != mix:
            problems.append(f"{kind}: {mix} mix sets in CAST, {len(sets)} in MIX")
        for i, botten in enumerate(sets):
            for bn in botten:
                if not bn.startswith(f"mix_{i}_") or bn not in namen:
                    problems.append(f"{kind}: mix bone {bn} of set {i} is missing or misnamed")
        vreemd = sorted(n for n in namen if n.startswith("mix_") and not any(n in s for s in sets))
        if vreemd:
            problems.append(f"{kind}: mix bones that are in no set: {vreemd}")
    problems += wezens.check(h, model_check)
    # the Java tables say the same
    java = _java("CastSchurk.java")
    if java is None:
        problems.append("CastSchurk.java is missing")
    else:
        for kind, (basis, lijf, mix, door, gloed, anims) in CAST.items():
            m = re.search(r"^\s*" + kind.upper() + r"\((.*?)\)[,;]\s*$", java, re.M)
            if not m:
                problems.append(f"CastSchurk.java has no row {kind.upper()}")
                continue
            wil = f'Kind.{basis.upper()}, "{lijf}", {mix}, {str(door).lower()}, {str(gloed).lower()}, ' + ", ".join(f'"{n}:{t}"' for n, t in anims)
            if " ".join(m.group(1).split()) != wil:
                problems.append(f"CastSchurk.{kind.upper()} says ({m.group(1)}), the python table ({wil})")
        for kind, sets in MIX.items():
            for botten in sets:
                for bn in botten:
                    if f'"{bn}"' not in java:
                        problems.append(f"CastSchurk.java does not list the mix bone {bn} of {kind}")
    java = _java("Wezens.java")
    if java is None:
        problems.append("Wezens.java is missing")
    else:
        for id, w in WEZENS.items():
            m = re.search(r"^\s*" + id.upper() + r"\((.*?)\)[,;]\s*$", java, re.M)
            wil = wezens.java_rij(id)
            if not m:
                problems.append(f"Wezens.java has no row {id.upper()}")
            elif " ".join(m.group(1).split()) != wil:
                problems.append(f"Wezens.{id.upper()} says ({m.group(1)}), the python table ({wil})")
    return problems


# =====================================================================================================================
# offline renders
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


def _render(wr, quads, yaw, pitch, size, margin=0.06, ss=2):
    """wiki_renders.render without its pixel loop (the same picture: orthographic, z-buffered, the same light)."""
    R = wr.rot_matrix(yaw, pitch)
    hoek = np.array([R @ (q.origin + a * q.u + b * q.v) for q in quads for a in (0, 1) for b in (0, 1)])
    lo, hi = hoek[:, :2].min(0), hoek[:, :2].max(0)
    W = size * ss
    schaal = W * (1 - 2 * margin) / (max(hi - lo) or 1)
    midden = (lo + hi) / 2
    licht = np.array([-0.35, 0.85, -0.4])
    licht /= np.linalg.norm(licht)
    PX, PY, Z, C = [], [], [], []
    for q in quads:
        if (R @ q.normal)[2] > 1e-6:
            continue
        tint = 0.62 + 0.38 * max(0.0, float(np.dot(q.normal, licht)))
        th, tw = q.tex.shape[:2]
        u0, v0, u1, v1 = q.uv
        nu = max(2, int(np.linalg.norm(R @ q.u) * schaal * 1.6) + 1)
        nv = max(2, int(np.linalg.norm(R @ q.v) * schaal * 1.6) + 1)
        A, B = np.meshgrid((np.arange(nu) + 0.5) / nu, (np.arange(nv) + 0.5) / nv)
        S = (q.origin + A[..., None] * q.u + B[..., None] * q.v) @ R.T
        rgba = q.tex[np.clip((v0 + B * (v1 - v0)) * th, 0, th - 1).astype(int), np.clip((u0 + A * (u1 - u0)) * tw, 0, tw - 1).astype(int)]
        px = ((S[..., 0] - midden[0]) * schaal + W / 2).astype(int)
        py = (-(S[..., 1] - midden[1]) * schaal + W / 2).astype(int)
        m = (px >= 0) & (px < W) & (py >= 0) & (py < W) & (rgba[..., 3] > 20)
        PX.append(px[m]), PY.append(py[m]), Z.append(S[..., 2][m]), C.append(rgba[m][:, :3] * tint)
    px, py, z, c = np.concatenate(PX), np.concatenate(PY), np.concatenate(Z), np.concatenate(C)
    volgorde = np.argsort(-z, kind="stable")                      # far first: the nearest point is written last
    beeld = np.zeros((W, W, 4), np.float32)
    beeld[py[volgorde], px[volgorde], :3] = c[volgorde]
    beeld[py[volgorde], px[volgorde], 3] = 255
    return Image.fromarray(np.clip(beeld, 0, 255).astype(np.uint8)).resize((size, size), Image.LANCZOS)


def _tegel(wr, geo, tex, hide, pose, out, hoeken, maat):
    for b in geo["minecraft:geometry"][0]["bones"]:
        if pose and b["name"] in pose:
            b["rotation"] = pose[b["name"]]
    pad = os.path.join(out, "_tmp.geo.json")
    json.dump(geo, open(pad, "w", encoding="utf-8"))
    q = wr.geo_quads(pad, np.asarray(tex.convert("RGBA")).astype(np.float32), hide=hide)
    os.remove(pad)
    return [_render(wr, q, yaw, pitch, maat) for yaw, pitch in hoeken]


def preview(out, alleen=None):
    """Renders into out/: one PNG per character (<name>.png: front, three-quarter, side-behind), and sheets of them."""
    import copy
    vang = _Vang()
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    hoeken, maat = ((0, -6), (32, -14), (150, -16)), 340
    rijen = []
    for kind in CAST:
        if alleen and kind not in alleen:
            continue
        blad, _ = _kind(vang, kind)
        tex = Image.fromarray(blad.img)
        sets = MIX.get(kind, [[]])
        for i, eigen in enumerate(sets):
            hide = tuple(mikas.VERBORGEN.get(kind, ())) + tuple(n for j, s in enumerate(sets) if j != i for n in s)
            naam = kind if len(sets) == 1 else f"{kind}_mix_{i}"
            rijen.append((naam, _tegel(wr, copy.deepcopy(blad.geo_file), tex, hide, None, out, hoeken, maat)))
    for naam, geo, tex, hide, pose in wezens.voorbeelden(vang, alleen):
        rijen.append((naam, _tegel(wr, copy.deepcopy(geo), tex, hide, pose, out, hoeken, maat)))
    for naam, tegels in rijen:
        strook = Image.new("RGBA", (maat * len(tegels), maat), (236, 232, 240, 255))
        for i, t in enumerate(tegels):
            strook.alpha_composite(t, (i * maat, 0))
        strook.save(os.path.join(out, f"{naam}.png"))
    return [n for n, _ in rijen]


if __name__ == "__main__":
    print("\n".join(preview(sys.argv[1] if len(sys.argv) > 1 else ".", sys.argv[2:] or None)))
