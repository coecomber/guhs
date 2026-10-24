"""
1.7.0 "Onder de Wol": the creatures of the new caves of the Guhmension (Java: nl.juiced.guhs.feature.onderwol.wezens).

  - de Grotguh (GuhVariant.GROTGUH): a fur colour only (no bones): deep pink/magenta with little crystal specks; the specks
    are also its glowmask (guh_grotguh_glowmask.png, drawn full-bright by OnderWolWezensClient), so they glow in the dark.
    Plus its sleeping texture (guh_slaap/guh_grotguh.png).
  - the cave molguh: the das-molguh of the Guheinde digs through the stone of the tag guhs:onderwol/molguh_grond.
  - the glimwormpjes of the Glitterspelonk (entity guhs:grotglimworm): the safari glimworm's baby model, nothing to make.

English: tools/lang/en/c190_onderwol_wezens.json.
"""
import os

import numpy as np
from PIL import Image

GROT_VACHT = (196, 86, 150)
SPIKKELS = [(255, 226, 246), (236, 200, 255), (255, 246, 252), (214, 236, 255)]
SPIKKEL_KANS = 0.022      # share of the fur's model pixels that get a crystal speck

TEKSTEN = {
    "entity.guhs.guh.grotguh": "Grotguh",
    "gui.guhs.guhdex.rarity.grotguh": "Zeldzaamheid: Alleen diep in de grotten van de Guhmension",
    "gui.guhs.guhdex.info.grotguh": "Een donkerroze guh met kleine kristalspikkeltjes in zijn vacht, die in het donker zachtjes "
                                    "glinsteren. Woont onder de wol, het liefst in de Glitterspelonk. Zie je iets twinkelen in "
                                    "een grot? Grote kans dat het een Grotguh is, njeg! Tem hem met kaasknabbels.",
    "entity.guhs.grotglimworm": "Glimwormpje",
}


def grotguh(base, v):
    """guh_grotguh.png and its glowmask: the guh recoloured, then crystal specks (one model pixel each, 4x4 texels) on the
    fur, with a bright middle. Returns (texture, glowmask)."""
    img = v.recolour_fur(base, GROT_VACHT)
    a = np.asarray(img).astype(np.int32)
    fur = (np.abs(np.asarray(base).astype(np.int32)[..., :3] - np.array(v.FUR)).sum(-1) < 40) & (a[..., 3] > 0)
    rng = np.random.default_rng(17000)
    glow = np.zeros_like(a)
    h, w = fur.shape
    for y in range(0, h, 4):
        for x in range(0, w, 4):
            if not fur[y:y + 4, x:x + 4].all() or rng.random() >= SPIKKEL_KANS:
                continue
            c = np.array(SPIKKELS[rng.integers(len(SPIKKELS))])
            a[y:y + 4, x:x + 4, :3] = c * 0.85
            a[y + 1:y + 3, x + 1:x + 3, :3] = np.minimum(c + 20, 255)
            glow[y:y + 4, x:x + 4, :3] = c * 0.7
            glow[y + 1:y + 3, x + 1:x + 3, :3] = c
            glow[y:y + 4, x:x + 4, 3] = 255
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)), Image.fromarray(glow.astype(np.uint8))


def textures(h):
    import make_guh_variants as v
    from features import bio_dieren_modellen as bm
    base = Image.open(os.path.join(h.TEX, "entity", "guh.png")).convert("RGBA")
    tex, glow = grotguh(base, v)
    h.save(tex, "entity", "guh_grotguh.png")
    h.save(glow, "entity", "guh_grotguh_glowmask.png")
    h.save(bm.slaap(tex, base, (120, 40, 90, 255)), "entity", "guh_slaap", "guh_grotguh.png")


def build(h):
    for k, t in TEKSTEN.items():
        h.NL[k] = t
    # the stone a cave molguh digs through like earth (DasMolguhEntity.ONDERWOL_GROND): the viltsteen and knuffelsteen layers
    h.add_tag("guhs/tags/block/onderwol/molguh_grond", ["guhs:viltsteen", "guhs:knuffelsteen", "guhs:guhklei"])
    textures(h)


def _direct():
    """Standalone (python tools/features/onderwol_wezens.py from the project root): the textures, the tag and the texts
    (nl_nl, and en_us from the English overlay), without the whole make_resources run."""
    import json
    import sys
    sys.path.insert(0, "tools")
    import make_v2 as h
    build(h)
    with open(os.path.join("tools", "lang", "en", "c190_onderwol_wezens.json"), encoding="utf-8") as f:
        en = json.load(f)
    for taal, extra in (("nl_nl", TEKSTEN), ("en_us", {k: en.get(k, t) for k, t in TEKSTEN.items()})):
        p = os.path.join("src", "main", "resources", "assets", "guhs", "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            d = json.load(f)
        d.update(extra)
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(d, f, indent=2, ensure_ascii=False)
            f.write("\n")
    print(f"onderwol_wezens: Grotguh textures, molguh tag, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
