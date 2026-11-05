"""Hoogkit: render- en bouwkit voor zone 5 "Hoogvads" (Guhs 1.10+, Het Vadsrijk).

Bovenop kopieën van de schemerwortel2-kit (schemerkit/vrkit/erfkit/mijnkit/vibekit/mcrender/mcextra; alleen gekopieerd).
Assets (in volgorde):
  jars/hv-concept.zip       nieuwe concept-blokken (gemaakt door concept_blokken.py; mag ontbreken)
  jars/guhs-assets-kint.zip alle guhs-assets uit guhs_vadsrijk/k-int (gezipt, alleen gelezen)
  vanilla 26.1.2 client-jar (PrismLauncher)
"""
import os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import vibekit as V  # noqa: E402

# [zone 5 kaart] the asset zips stay outside the repo (MineScapeStory/renders-hoogvads/_bron/jars), only read for block shapes
JARDIR = os.environ.get("GUHS_HV_JARS") or os.path.join(os.path.abspath(os.path.join(HERE, *([".."] * 6))), "renders-hoogvads", "_bron", "jars")
CONCEPT_ZIP = os.path.join(JARDIR, "hv-concept.zip")
V.JARS = ([CONCEPT_ZIP] if os.path.exists(CONCEPT_ZIP) else []) + [
    os.path.join(JARDIR, "guhs-assets-kint.zip"),
    os.path.join(os.environ.get("APPDATA", ""),
                 "PrismLauncher/libraries/com/mojang/minecraft/26.1.2/minecraft-26.1.2-client.jar")]
import mijnkit as _M  # noqa: E402,F401  (GUH_BASIS-patch; zet zelf V.JARS -> hieronder opnieuw)
V.JARS = ([CONCEPT_ZIP] if os.path.exists(CONCEPT_ZIP) else []) + [
    os.path.join(JARDIR, "guhs-assets-kint.zip"),
    os.path.join(os.environ.get("APPDATA", ""),
                 "PrismLauncher/libraries/com/mojang/minecraft/26.1.2/minecraft-26.1.2-client.jar")]
V.OUT = os.path.abspath(os.path.join(HERE, ".."))


# [zone 5 kaart] the Hoogvads blocks of the mod itself (vadsrijk_t_z5blokken.py: concept names without hv_) come first, read from
# the repo's own resources (zipped into tools/vadsrijk/_cache/z5 so the kit's zip reader can use them)
def _repo_zip():
    import json as _json
    import zipfile as _zf
    root = os.path.abspath(os.path.join(HERE, "..", "..", "..", ".."))
    a = os.path.join(root, "src", "main", "resources", "assets", "guhs")
    uit = os.path.join(root, "tools", "vadsrijk", "_cache", "z5", "hoogvads-assets.zip")
    os.makedirs(os.path.dirname(uit), exist_ok=True)
    try:
        sys.path.insert(0, os.path.join(root, "tools", "features"))
        import vadsrijk_t_z5blokken as ZB
        namen = list(ZB.BLOKKEN)
    except Exception:  # noqa
        return None
    gedaan = set()
    with _zf.ZipFile(uit, "w") as z:
        for n in namen:
            p = os.path.join(a, "blockstates", n + ".json")
            if not os.path.exists(p):
                continue
            z.write(p, f"assets/guhs/blockstates/{n}.json")
            s = open(p, encoding="utf-8").read()
            for m in set(__import__("re").findall(r'guhs:block/([a-z0-9_/]+)', s)):
                mp = os.path.join(a, "models", "block", m + ".json")
                if os.path.exists(mp) and m not in gedaan:
                    gedaan.add(m)
                    z.write(mp, f"assets/guhs/models/block/{m}.json")
        td = os.path.join(a, "textures", "block", "hoogvads")
        for f in os.listdir(td) if os.path.isdir(td) else []:
            z.write(os.path.join(td, f), f"assets/guhs/textures/block/hoogvads/{f}")
    return uit


_RZ = _repo_zip()
if _RZ:
    V.JARS.insert(0, _RZ)
    V._res = V._bk = None
