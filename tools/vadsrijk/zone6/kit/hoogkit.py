"""Kit base for zone 6 "Het Zijdeland" (Guhs 1.10, Het Vadsrijk). [zone 6 kaart] copy of renders-zijdeland/_bron/hoogkit.py.

Assets (read only, never copied; like zone 5):
  MineScapeStory/renders-hoogvads/_bron/jars/hv-concept.zip       the zone-5 concept blocks (hv_*), only for renders
  MineScapeStory/renders-hoogvads/_bron/jars/guhs-assets-kint.zip  all Guhs assets of k-int
  the vanilla 26.1.2 client jar (PrismLauncher)
  + the repo's own zone-5 (vadsrijk_t_z5blokken) and zone-6 (vadsrijk_u_z6blokken) blocks, zipped into tools/vadsrijk/_cache/z6.
Render output (opname): MineScapeStory/renders-zijdeland/ronde2 (GUHS_Z6_RENDERS overrides).
"""
import os, sys, re
import zipfile as _zf

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import vibekit as V  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", "..", "..", ".."))
MSS = os.path.abspath(os.path.join(ROOT, "..", ".."))           # MineScapeStory
JARDIR = os.environ.get("GUHS_HV_JARS") or os.path.join(MSS, "renders-hoogvads", "_bron", "jars")
CONCEPT_ZIP = os.path.join(JARDIR, "hv-concept.zip")


def _jars():
    return ([CONCEPT_ZIP] if os.path.exists(CONCEPT_ZIP) else []) + [
        os.path.join(JARDIR, "guhs-assets-kint.zip"),
        os.path.join(os.environ.get("APPDATA", ""),
                     "PrismLauncher/libraries/com/mojang/minecraft/26.1.2/minecraft-26.1.2-client.jar")]


V.JARS = _jars()
import mijnkit as _M  # noqa: E402,F401  (GUH_BASIS patch; sets V.JARS itself -> set again below)
V.JARS = _jars()
RENDER_OUT = os.environ.get("GUHS_Z6_RENDERS") or os.path.join(MSS, "renders-zijdeland", "ronde2")
V.OUT = RENDER_OUT
os.makedirs(RENDER_OUT, exist_ok=True)


def _repo_zip():
    """The repo's own zone-5 + zone-6 blocks (blockstates, models, textures) as a zip for the kit's asset reader."""
    a = os.path.join(ROOT, "src", "main", "resources", "assets", "guhs")
    uit = os.path.join(ROOT, "tools", "vadsrijk", "_cache", "z6", "repo-assets.zip")
    os.makedirs(os.path.dirname(uit), exist_ok=True)
    sys.path.insert(0, os.path.join(ROOT, "tools", "features"))
    namen = []
    for mod in ("vadsrijk_t_z5blokken", "vadsrijk_u_z6blokken"):
        try:
            namen += list(__import__(mod).BLOKKEN)
        except Exception:  # noqa
            pass
    if not namen:
        return None
    gedaan, tex = set(), set()
    with _zf.ZipFile(uit, "w") as z:
        for n in namen:
            p = os.path.join(a, "blockstates", n + ".json")
            if not os.path.exists(p):
                continue
            z.write(p, f"assets/guhs/blockstates/{n}.json")
            s = open(p, encoding="utf-8").read()
            todo = list(set(re.findall(r'guhs:block/([a-z0-9_/]+)', s)))
            while todo:
                m = todo.pop()
                mp = os.path.join(a, "models", "block", m + ".json")
                if not os.path.exists(mp) or m in gedaan:
                    continue
                gedaan.add(m)
                z.write(mp, f"assets/guhs/models/block/{m}.json")
                ms = open(mp, encoding="utf-8").read()
                for t in re.findall(r'"guhs:block/([a-z0-9_/]+)"', ms):
                    if os.path.exists(os.path.join(a, "models", "block", t + ".json")):
                        todo.append(t)
                    tex.add(t)
        for t in tex:
            tp = os.path.join(a, "textures", "block", t + ".png")
            if os.path.exists(tp):
                z.write(tp, f"assets/guhs/textures/block/{t}.png")
                if os.path.exists(tp + ".mcmeta"):
                    z.write(tp + ".mcmeta", f"assets/guhs/textures/block/{t}.png.mcmeta")
    return uit


_RZ = _repo_zip()
if _RZ:
    V.JARS.insert(0, _RZ)
    V._res = V._bk = None
