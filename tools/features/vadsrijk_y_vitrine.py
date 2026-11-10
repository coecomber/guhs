"""
1.10 "Het Vadsrijk", round Y, slice Y-VITRINE (Java: feature/vadsrijk/vitrine): the Vadsrijk-vitrine, the one trophy block for
your survival base that shows your Vadsrijk character live.

  - block model vr_vitrine: a plum-wood pedestal with gold trim, a little velvet stand, gold corner posts, glass sides and a
    glass top in a gold rim, a gold name plaque on the front (translucent render type: the statue shows through the glass);
  - the Knuffelorde emblem (item model guhs:vr_vitrine_embleem, no item of its own): what the case shows when the owner has no
    mount and no pet (the renderer puts it on a paper stack via the item_model component);
  - blockstate (4 facings), item model, loot table (drops itself);
  - all texts (Dutch; en_us English, also in the overlay chunk tools/lang/en/c460_vr_y_vitrine.json).

Standalone (from the project root): PYTHONPATH="tools;tools/features" python tools/features/vadsrijk_y_vitrine.py
"""
import json
import os

import numpy as np
from PIL import Image

ID = "vr_vitrine"
TEX = "vadsrijk/vitrine"       # textures/block/vadsrijk/vitrine/*.png, textures/item/vadsrijk/vitrine/*.png
K = "vadsrijk.vitrine."

TEKSTEN = {
    f"block.guhs.{ID}": ("Vadsrijk-vitrine", "Chonkdom Showcase"),
    f"block.guhs.{ID}.tip": ("Een trofeekastje voor je basis: laat live je held uit Het Vadsrijk zien.",
                             "A trophy case for your base: shows your hero from the Chonkdom, live."),
    f"block.guhs.{ID}.tip2": ("Alleen buiten Het Vadsrijk neer te zetten. Klik erop voor een overzicht.",
                              "Can only be placed outside the Chonkdom. Click it for an overview."),
    K + "niet_hier": ("De vitrine hoort in je eigen basis: buiten Het Vadsrijk.", "The showcase belongs in your own base: outside the Chonkdom."),
    K + "geplaatst": ("Je Vadsrijk-vitrine staat! Hij laat je held zien en ververst vanzelf zolang je online bent.",
                      "Your Chonkdom Showcase is up! It shows your hero and updates by itself while you are online."),
    K + "bord.vrij": ("Klik om deze vitrine van jou te maken", "Click to make this showcase yours"),
    K + "bord.titel": ("%s in Het Vadsrijk", "%s in the Chonkdom"),
    K + "bord.leeg": ("Nog geen held: ga eerst Het Vadsrijk in!", "No hero yet: visit the Chonkdom first!"),
    K + "klasse": ("%s · level %s", "%s · level %s"),
    K + "klasse.spec": ("%s (%s) · level %s", "%s (%s) · level %s"),
    K + "raid": ("Raid: %sx gewonnen", "Raid: won %s times"),
    K + "eerste": ("Eerste zone voltooid: %s", "First zone completed: %s"),
    K + "chat.kop": ("Vadsrijk-vitrine van %s", "%s's Chonkdom Showcase"),
    K + "chat.leeg": ("%s heeft Het Vadsrijk nog niet bezocht.", "%s hasn't visited the Chonkdom yet."),
    K + "chat.zones": ("Zones voltooid: %s van %s", "Zones completed: %s of %s"),
    K + "chat.zone.klaar": (" ✔ Zone %s: %s", " ✔ Zone %s: %s"),
    K + "chat.zone.bezig": (" · Zone %s: %s (nog bezig)", " · Zone %s: %s (in progress)"),
    K + "chat.zone.dicht": (" ? Zone %s: binnenkort", " ? Zone %s: coming soon"),
    K + "chat.beeld": ("In de vitrine: %s", "In the showcase: %s"),
    K + "chat.beeld.embleem": ("In de vitrine: het embleem van De Knuffelorde", "In the showcase: the emblem of the Snuggle Order"),
    K + "chat.offline": ("(%s is nu niet online: dit is de stand van de laatste keer.)", "(%s is not online now: this is how things stood last time.)"),
}


# ======================================================================================================================
# textures
# ======================================================================================================================
def _ruis(c, n=16, sterkte=6, seed=0):
    rng = np.random.default_rng(seed)
    a = np.zeros((n, n, 4), np.float32)
    a[..., :3] = np.array(c, np.float32) + rng.normal(0, sterkte, (n, n, 1))
    a[..., 3] = 255
    return a


def _sokkel():
    """Plum wood: vertical grain, a darker groove line and a gold inlay band."""
    a = _ruis((112, 58, 92), 16, 5, 4601)
    for x in range(16):
        a[:, x, :3] += 8 * np.sin(x * 1.7)
    a[3, :, :3] = (84, 40, 70)
    a[12, :, :3] = (84, 40, 70)
    a[7:9, :, :3] = (236, 186, 76)
    a[7, ::3, :3] = (255, 226, 140)
    return a


def _goud():
    a = _ruis((236, 186, 70), 16, 9, 4602)
    a[0, :, :3] = (255, 232, 150)
    a[15, :, :3] = (176, 120, 40)
    return a


def _glas():
    """Nearly clear glass with a faint lilac tint, a light rim and two diagonal shine streaks."""
    a = np.zeros((16, 16, 4), np.float32)
    a[..., :3] = (228, 220, 255)
    a[..., 3] = 46
    a[0, :, 3] = a[15, :, 3] = a[:, 0, 3] = a[:, 15, 3] = 120
    for i in range(16):
        for d, al in ((0, 150), (1, 90)):
            x, y = i, 15 - i - 4 + d
            if 0 <= y < 16 and 2 < i < 9:
                a[y, x, :3] = (255, 255, 255)
                a[y, x, 3] = al
        x, y = i, 15 - i + 5
        if 0 <= y < 16 and 6 < i < 11:
            a[y, x, :3] = (255, 255, 255)
            a[y, x, 3] = 110
    return a


def _fluweel():
    a = _ruis((176, 40, 84), 16, 7, 4603)
    yy, xx = np.mgrid[0:16, 0:16]
    a[((yy - 7.5) ** 2 + (xx - 7.5) ** 2) < 9, :3] *= 1.12
    return a


def _plaquette():
    """The gold plaque on the front: a dark plate in a gold frame with a tiny pink heart."""
    a = _goud()
    a[3:13, 2:14, :3] = (70, 34, 58)
    a[3, 2:14, :3] = (255, 226, 140)
    hart = ["0110110", "1111111", "1111111", "0111110", "0011100", "0001000"]
    for y, r in enumerate(hart):
        for x, c in enumerate(r):
            if c == "1":
                a[5 + y, 4 + x + 1, :3] = (255, 120, 180)
    return a


def _embleem():
    """The Knuffelorde emblem (32x32): a pink heart shield with a gold rim, a small gold crown and a white plush guh-face."""
    n = 32
    a = np.zeros((n, n, 4), np.float32)
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    cx = 15.5
    # heart shape: two circles + a triangle to the point
    l = ((xx - (cx - 5.5)) ** 2 + (yy - 13) ** 2) < 7.2 ** 2
    r = ((xx - (cx + 5.5)) ** 2 + (yy - 13) ** 2) < 7.2 ** 2
    t = (yy >= 13) & (yy <= 29) & (np.abs(xx - cx) <= (29 - yy) * 0.82)
    vorm = l | r | t
    binnen_l = ((xx - (cx - 5.5)) ** 2 + (yy - 13) ** 2) < 5.6 ** 2
    binnen_r = ((xx - (cx + 5.5)) ** 2 + (yy - 13) ** 2) < 5.6 ** 2
    binnen_t = (yy >= 13) & (yy <= 26.5) & (np.abs(xx - cx) <= (26.5 - yy) * 0.82)
    binnen = binnen_l | binnen_r | binnen_t
    a[vorm] = (236, 186, 70, 255)
    a[vorm & (yy < 12)] = (255, 222, 130, 255)
    a[binnen] = (255, 120, 182, 255)
    a[binnen & (yy > 20)] = (232, 92, 156, 255)
    # the face
    for (x, y) in ((12, 15), (19, 15)):
        a[y:y + 2, x:x + 1] = (40, 24, 40, 255)
        a[y, x] = (255, 255, 255, 255)
    a[18, 15:17] = (120, 40, 80, 255)
    a[17, 10] = a[17, 21] = (255, 190, 220, 255)
    # the crown on top
    for x in range(10, 22):
        a[4:6, x] = (255, 214, 80, 255)
    for x in (10, 15, 16, 21):
        a[1:4, x] = (255, 214, 80, 255)
    a[2, 15:17] = (255, 120, 182, 255)
    rng = np.random.default_rng(4604)
    m = a[..., 3] > 0
    a[m, :3] += rng.normal(0, 4, (int(m.sum()), 1))
    return a


BLOK_TEX = {"sokkel": _sokkel, "goud": _goud, "glas": _glas, "fluweel": _fluweel, "plaquette": _plaquette}


# ======================================================================================================================
# the block model
# ======================================================================================================================
def _el(frm, to, tex, voor=None):
    faces = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        t = voor if (voor and f == "north") else tex
        face = {"texture": "#" + t}
        if voor and f == "north":
            face["uv"] = [0, 0, 16, 16]
        faces[f] = face
    return {"from": frm, "to": to, "faces": faces}


def elementen():
    e = [
        _el([1, 0, 1], [15, 3, 15], "sokkel"),                       # the pedestal
        _el([0.5, 3, 0.5], [15.5, 4, 15.5], "goud"),                 # its gold rim
        _el([5, 4, 5], [11, 4.5, 11], "fluweel"),                    # the little velvet stand (the statue stands on 4.5)
        _el([5, 0.8, 0.6], [11, 2.4, 1], "goud", "plaquette"),       # the name plaque on the front
    ]
    for x, z in ((1, 1), (14, 1), (1, 14), (14, 14)):                # gold corner posts
        e.append(_el([x, 4, z], [x + 1, 15, z + 1], "goud"))
    e += [                                                           # the glass sides
        _el([2, 4, 1.25], [14, 15, 1.75], "glas"),
        _el([2, 4, 14.25], [14, 15, 14.75], "glas"),
        _el([1.25, 4, 2], [1.75, 15, 14], "glas"),
        _el([14.25, 4, 2], [14.75, 15, 14], "glas"),
        _el([2, 15, 2], [14, 15.5, 14], "glas"),                     # the glass top
        _el([1, 15, 1], [15, 16, 2], "goud"), _el([1, 15, 14], [15, 16, 15], "goud"),   # its gold rim
        _el([1, 15, 2], [2, 16, 14], "goud"), _el([14, 15, 2], [15, 16, 14], "goud"),
        _el([7, 16, 7], [9, 16.5, 9], "goud"),                       # a little knob on top
    ]
    return e


FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def _schrijf(pad, obj):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    with open(pad, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def _png(pad, a):
    os.makedirs(os.path.dirname(pad), exist_ok=True)
    Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA").save(pad)


def assets(A, D, w=_schrijf):
    for naam, f in BLOK_TEX.items():
        _png(os.path.join(A, "textures", "block", *TEX.split("/"), f"{naam}.png"), f())
    tex = {t: f"guhs:block/{TEX}/{t}" for t in BLOK_TEX}
    tex["particle"] = tex["goud"]
    w(os.path.join(A, "models", "block", f"{ID}.json"),
      {"parent": "minecraft:block/block", "render_type": "minecraft:translucent", "textures": tex, "elements": elementen()})
    w(os.path.join(A, "blockstates", f"{ID}.json"),
      {"variants": {f"facing={f}": {"model": f"guhs:block/{ID}", **({"y": y} if y else {})} for f, y in FACING_Y.items()}})
    w(os.path.join(A, "items", f"{ID}.json"), {"model": {"type": "minecraft:model", "model": f"guhs:block/{ID}"}})
    w(os.path.join(D, "loot_table", "blocks", f"{ID}.json"), {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"guhs:{ID}"}],
                                               "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    # the emblem: an item model only (no item registered; the renderer uses the item_model component)
    _png(os.path.join(A, "textures", "item", *TEX.split("/"), "embleem.png"), _embleem())
    w(os.path.join(A, "models", "item", "vr_vitrine_embleem.json"),
      {"parent": "minecraft:item/generated", "textures": {"layer0": f"guhs:item/{TEX}/embleem"}})
    w(os.path.join(A, "items", "vr_vitrine_embleem.json"), {"model": {"type": "minecraft:model", "model": "guhs:item/vr_vitrine_embleem"}})


def build(h):
    assets(h.A, h.D, h.w)
    for k, (nl, en) in TEKSTEN.items():
        h.lang(k, en, nl)


def _direct():
    r = os.path.join("src", "main", "resources")
    a, d = os.path.join(r, "assets", "guhs"), os.path.join(r, "data", "guhs")
    assets(a, d)
    for taal, i in (("nl_nl", 0), ("en_us", 1)):
        p = os.path.join(a, "lang", f"{taal}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data.update({k: v[i] for k, v in TEKSTEN.items()})
        _schrijf(p, data)
    print(f"vadsrijk_y_vitrine: model + emblem, {len(TEKSTEN)} texts")


if __name__ == "__main__":
    _direct()
