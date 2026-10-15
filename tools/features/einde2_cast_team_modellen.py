"""
einde2, slice cast_team - the models of Team Guhvatar and their friends (CONTRACT_E2 11.4): twenty-two GuhNpcEntity.Kinds
(nineteen of 11.4 and the three Zuka looks of 11.9), eight characters with looks and five plain ones.

  CAST       the table (the same rows as the Java enum feature/einde2/castteam/CastTeam; check() compares the two)
  build(h)   per Kind  geckolib/models/entity/guh_npc_<kind>.geo.json + textures/entity/npc_<kind>.png (+ _glowmask.png),
             per CHARACTER  geckolib/animations/entity/guh_npc_<basis>.animation.json, and per other body of its looks
             guh_npc_<basis>_<lijf>.animation.json (Zuka: guh_npc_zuka_zit for ZUKA_TEAM and ZUKA_VUURHEER)
  check(h)   every file is there, every animation holds idle, praat and the names of its row, every bone an animation
             moves exists in every look of the character, no base bone was lost, the Java table says the same
  preview(out)   offline renders (einde2_cast_team_kit.quads + render): per Kind the idle pose from the
             front, three-quarter and behind and its first move; per character a strip of all its moves

How the characters look: einde2_cast_team_figuren.py; how they move: einde2_cast_team_anim.py; the tools: einde2_cast_team_kit.py.
"""
import json
import os
import re
import sys

import numpy as np
from PIL import Image

from features import einde2_cast_team_anim as an
from features import einde2_cast_team_figuren as fig
from features import einde2_cast_team_kit as ck

# kind -> (basis, lijf, Kind.scale, doorzichtig, gloed); the order of the Kind block in GuhNpcEntity
CAST = {
    "aang_guh": ("aang_guh", "zit", 0.9, False, False),
    "katara_guh": ("katara_guh", "zit", 1.0, False, False),
    "sokka_guh": ("sokka_guh", "zit", 1.0, False, False),
    "toph_guh": ("toph_guh", "zit", 0.8, False, False),
    "zuka": ("zuka", "mika", 1.0, False, False),
    "zuka_prins": ("zuka", "mika", 1.0, False, False),
    "zuka_vuurheer": ("zuka", "zit", 1.05, False, False),
    "zuka_balling": ("zuka", "mika", 1.0, False, False),
    "zuka_blauwe_geest": ("zuka", "mika", 1.0, False, False),
    "zuka_team": ("zuka", "zit", 1.0, False, False),
    "oom_miroh": ("oom_miroh", "zit", 1.0, False, False),
    "oom_miroh_gevangene": ("oom_miroh", "zit", 1.0, False, False),
    "oom_miroh_lotus": ("oom_miroh", "zit", 1.0, False, False),
    "suki_guh": ("suki_guh", "zit", 1.0, False, False),
    "suki_guh_gevangene": ("suki_guh", "zit", 1.0, False, False),
    "yue_guh": ("yue_guh", "zit", 0.95, False, False),
    "yue_guh_maan": ("yue_guh", "zit", 0.95, True, True),
    "veerguh": ("veerguh", "zit", 1.0, False, False),
    "scheepsbouwerguh": ("scheepsbouwerguh", "zit", 1.05, False, False),
    "kroegbaasguh": ("kroegbaasguh", "zit", 1.1, False, False),
    "kapiteinguh": ("kapiteinguh", "zit", 0.95, False, False),
    "ringenraceguh": ("ringenraceguh", "zit", 1.0, False, False),
}
BASISSEN = list(dict.fromkeys(row[0] for row in CAST.values()))
JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "einde2", "castteam", "CastTeam.java")
ENTITEIT = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "entity", "GuhNpcEntity.java")


def namen(kind):
    """The animations of a Kind besides idle: "praat" first, then the names of its character's row."""
    return ("praat",) + an.NAMEN[CAST[kind][0]]


def _geo(h, kind):
    return os.path.join(h.A, "geckolib", "models", "entity", f"guh_npc_{kind}.geo.json")


def _anim(h, basis, lijf=None):
    return os.path.join(h.A, "geckolib", "animations", "entity", f"{an.bestand_naam(basis, lijf)}.animation.json")


def build(h):
    for kind in CAST:
        fig.teken(h, kind)
    for basis in BASISSEN:
        for lijf in an.lijven(basis):
            h.w(_anim(h, basis, lijf), an.bestand(basis, lijf))


def check(h):
    fout = []
    for kind, (basis, lijf, schaal, doorzichtig, gloed) in CAST.items():
        tex = os.path.join(h.TEX, "entity", f"npc_{kind}.png")
        if lijf not in an.lijven(basis):
            fout.append(f"{kind}: body {lijf}, but its character has animations for {an.lijven(basis)} only")
            continue
        ontbreekt = [pad for pad in (_geo(h, kind), tex, _anim(h, basis, lijf)) if not os.path.exists(pad)]
        fout += [f"{kind}: {pad} is missing" for pad in ontbreekt]
        if gloed != os.path.exists(os.path.join(h.TEX, "entity", f"npc_{kind}_glowmask.png")):
            fout.append(f"{kind}: its glowmask is {'missing' if gloed else 'not expected'}")
        if ontbreekt:
            continue
        geo = json.load(open(_geo(h, kind), encoding="utf-8"))["minecraft:geometry"][0]
        botten = {b["name"] for b in geo["bones"]}
        if geo["description"]["identifier"] != f"geometry.guh_npc_{kind}":
            fout.append(f"{kind}: identifier {geo['description']['identifier']}")
        for b in ck.BASISBOTTEN[lijf]:
            if b not in botten:
                fout.append(f"{kind}: base bone {b} is gone")
        for b in botten - set(ck.BASISBOTTEN[lijf]):
            if b.startswith("mix_") or "_" not in b:
                fout.append(f"{kind}: bone {b} is not named <basis>_<ding>")
        anims = json.load(open(_anim(h, basis, lijf), encoding="utf-8"))["animations"]
        for naam in ("idle",) + namen(kind):
            a = anims.get(f"animation.guh_sitting.{naam}")
            if a is None:
                fout.append(f"{kind}: animation {naam} is missing")
                continue
            if naam != "idle" and int(round(a["animation_length"] * 20)) != (an.duur(basis, naam) or int(round(a["animation_length"] * 20))):
                fout.append(f"{kind}: animation {naam} on its {lijf} body is not as long as the table says")
            for b in a["bones"]:
                if b not in botten:
                    fout.append(f"{kind}: animation {naam} moves bone {b}, which the model does not have")
            for prop in an.PROPS.get(basis, ()):
                if prop not in a["bones"]:
                    fout.append(f"{kind}: animation {naam} does not hide prop {prop}")
        if len(anims) != len(namen(kind)) + 1:
            fout.append(f"{kind}: {len(anims)} animations in the file, the row names {len(namen(kind)) + 1}")
        img = Image.open(tex)
        if img.size != (512, 512):
            fout.append(f"{kind}: texture is {img.size}")
        if doorzichtig:
            alfa = np.asarray(img.convert("RGBA"))[..., 3]
            if not ((alfa[alfa > 0] >= 150) & (alfa[alfa > 0] <= 190)).all():
                fout.append(f"{kind}: a see-through kind is painted with alpha 150-190")
    fout += _java()
    if fout:
        raise SystemExit("einde2_cast_team_modellen:\n  " + "\n  ".join(fout))
    return f"{len(CAST)} kinds, {len(BASISSEN)} characters"


def _java():
    """The Java table and the Kind block say what CAST says."""
    fout = []
    if not os.path.exists(JAVA):
        return [f"{JAVA} is missing"]
    bron = open(JAVA, encoding="utf-8").read()
    rijen = re.findall(r'^\s+([A-Z_]+)\(GuhNpcEntity\.Kind\.([A-Z_]+), GuhNpcEntity\.Kind\.([A-Z_]+), "(\w+)", (true|false), (true|false),\s+"([^"]*)"\)[,;]', bron, re.M)
    java = {r[1].lower(): (r[2].lower(), r[3], r[4] == "true", r[5] == "true", r[6]) for r in rijen}
    if list(java) != list(CAST):
        fout.append(f"CastTeam.java lists {list(java)}, CAST {list(CAST)}")
    for kind, (basis, lijf, schaal, doorzichtig, gloed) in CAST.items():
        verwacht = (basis, lijf, doorzichtig, gloed, ",".join(f"{n}:{an.duur(basis, n)}" for n in namen(kind)))
        if kind in java and java[kind] != verwacht:
            fout.append(f"CastTeam.java {kind}: {java[kind]}, CAST says {verwacht}")
    blok = re.search(r"// <einde2_cast_team>(.*?)// </einde2_cast_team>", open(ENTITEIT, encoding="utf-8").read(), re.S)
    kinds = {n.lower(): float(s) for n, s in re.findall(r"([A-Z_]+)\(([0-9.]+)f\)", blok.group(1))} if blok else {}
    if kinds != {k: row[2] for k, row in CAST.items()}:
        fout.append(f"the Kind block of GuhNpcEntity says {kinds}")
    return fout


def java_rijen():
    """The rows of the Java enum, as text (to paste when CAST or an animation changes)."""
    out = []
    for kind, (basis, lijf, schaal, doorzichtig, gloed) in CAST.items():
        lijst = ",".join(f"{n}:{an.duur(basis, n)}" for n in namen(kind))
        out.append(f'    {kind.upper()}(GuhNpcEntity.Kind.{kind.upper()}, GuhNpcEntity.Kind.{basis.upper()}, "{lijf}", '
                   f'{str(doorzichtig).lower()}, {str(gloed).lower()},\n            "{lijst}"),')
    return "\n".join(out)


# =====================================================================================================================
# offline previews
# =====================================================================================================================
MAAT = 340
ACHTER = (236, 232, 240, 255)


def _model(kind):
    geo_dir = os.path.join("src", "main", "resources", "assets", "guhs", "geckolib", "models", "entity")
    tex_dir = os.path.join("src", "main", "resources", "assets", "guhs", "textures", "entity")
    geo = json.load(open(os.path.join(geo_dir, f"guh_npc_{kind}.geo.json"), encoding="utf-8"))["minecraft:geometry"][0]
    tex = np.asarray(Image.open(os.path.join(tex_dir, f"npc_{kind}.png")).convert("RGBA")).astype(np.float32)
    return geo, tex


def _strip(tegels):
    strip = Image.new("RGBA", (MAAT * len(tegels), MAAT), ACHTER)
    for i, t in enumerate(tegels):
        strip.alpha_composite(t, (i * MAAT, 0))
    return strip


def preview(out, alleen=None):
    """Per Kind <kind>.png: the idle pose from the front, three-quarter and behind, and its first move half-way; per
    character <basis>_moves.png: every named animation half-way (three-quarter); and overzicht_<n>.png, four kinds a sheet."""
    sys.path.insert(0, "tools")
    import wiki_renders as wr
    os.makedirs(out, exist_ok=True)
    stroken = []
    for kind in CAST:
        if alleen and kind not in alleen and CAST[kind][0] not in alleen:
            continue
        basis = CAST[kind][0]
        geo, tex = _model(kind)
        anims = an.animaties(basis, CAST[kind][1])
        rust = ck.quads(geo, tex, ck.op(anims["idle"], 0.0))
        tegels = [ck.render(rust, yaw, pitch, MAAT, margin=0.05) for yaw, pitch in ((0, -8), (35, -14), (205, -16))]
        eerste = an.NAMEN[basis][0]
        tegels.append(ck.render(ck.quads(geo, tex, ck.op(anims[eerste], anims[eerste]["animation_length"] * 0.5)), 30, -12, MAAT, margin=0.05))
        strook = _strip(tegels)
        strook.save(os.path.join(out, f"{kind}.png"))
        stroken.append(strook)
        if kind == basis or kind == next(k for k, r in CAST.items() if r[0] == basis and r[1] == CAST[kind][1]):
            zetten = [ck.render(ck.quads(geo, tex, ck.op(anims[n], anims[n]["animation_length"] * (0.5 if n != "praat" else 0.25))), 30, -12, MAAT, margin=0.05)
                      for n in namen(kind)]
            _strip(zetten).save(os.path.join(out, f"{kind}_moves.png"))
    for s in range(0, len(stroken), 4):
        deel = stroken[s:s + 4]
        vel = Image.new("RGBA", (MAAT * 4, MAAT * len(deel)), ACHTER)
        for i, t in enumerate(deel):
            vel.alpha_composite(t, (0, i * MAAT))
        vel.save(os.path.join(out, f"overzicht_{s // 4}.png"))


if __name__ == "__main__":
    preview(sys.argv[1] if len(sys.argv) > 1 else ".", sys.argv[2:] or None)
