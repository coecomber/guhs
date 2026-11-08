"""Blokstudie voor Hoogvads: alle guhs-blokken (k-int) + alle vanilla-26.1-blokken langslopen, per blok de
hoofdtextuur + gemiddelde kleur bepalen, en contactvellen maken per kleurgroep/trefwoord.

python blokstudie.py            -> studie/*.png (contactvellen) + studie/blokken.tsv
"""
import os, sys, io, json, colorsys
import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import hoogkit as H  # noqa: E402  (zet de jars)

V = H.V
OUT = os.path.join(HERE, "studie")
os.makedirs(OUT, exist_ok=True)


def textuur_van(r, n):
    ns, p = n.split(":")
    bs = r.json(n, "blockstates")
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
        m = r.json(model if ":" in model else "minecraft:" + model, "models")
        if not m:
            break
        tx = m.get("textures", {})
        for k in ("all", "side", "front", "texture", "wool", "cross", "top", "end", "particle"):
            t = tx.get(k)
            if t and not t.startswith("#"):
                return t
        for t in tx.values():
            if not t.startswith("#"):
                return t
        model = m.get("parent")
    return None


def laad(r, t):
    ns, p = t.split(":") if ":" in t else ("minecraft", t)
    d = r.read(f"assets/{ns}/textures/{p}.png")
    if not d:
        return None
    im = Image.open(io.BytesIO(d)).convert("RGBA")
    if im.height > im.width:
        im = im.crop((0, 0, im.width, im.width))
    return im.resize((16, 16), Image.NEAREST) if im.width != 16 else im


def alle():
    r, _ = V.res()
    rij = []
    for zi, (z, names) in enumerate(zip(r.zips, r.names)):
        for n in sorted(names):
            if "/blockstates/" not in n or not n.endswith(".json"):
                continue
            ns = n.split("/")[1]
            p = n.split("/")[-1][:-5]
            rij.append(f"{ns}:{p}")
    rij = sorted(set(rij))
    data = []
    for b in rij:
        try:
            t = textuur_van(r, b)
        except Exception:
            t = None
        im = laad(r, t) if t else None
        if im is None:
            continue
        a = np.asarray(im, np.float32) / 255.0
        m = a[..., 3] > 0.5
        if m.sum() < 20:
            continue
        c = a[..., :3][m].mean(0)
        h, l, s = colorsys.rgb_to_hls(*c)
        var = float(a[..., :3][m].std())
        data.append((b, t, c, h, l, s, var, im))
    return data


def groep(d):
    b, t, c, h, l, s, var, im = d
    hd = h * 360
    if l > 0.78 and s < 0.35:
        return "wit_creme"
    if (hd >= 300 or hd < 15) and s > 0.2 and l > 0.45:
        return "roze"
    if 30 <= hd < 62 and s > 0.35 and l > 0.4:
        return "goud_geel"
    if 180 <= hd < 250 and l > 0.6:
        return "hemelblauw"
    return None


TREF = ["marmer", "kwarts", "quartz", "goud", "gold", "glas", "glass", "pilaar", "pillar", "zuil", "beeld", "statue",
        "kroon", "troon", "wolk", "cloud", "tegel", "tile", "dak", "roof", "banier", "banner", "vaandel", "vlag",
        "ornament", "chiseled", "polished", "calcite", "diorite", "kathedraal", "kerk", "orgel", "kaars", "candle",
        "kroonluchter", "luchter", "glasinlood", "roze", "parel", "pearl", "kristal", "bloesem", "cherry",
        "copper", "koper", "resin", "tuff", "brick", "baksteen", "smooth", "cut_", "walvis", "huid", "lamp", "lantaarn"]


def vel(lijst, naam, kol=12):
    try:
        f = ImageFont.truetype("segoeui.ttf", 11)
    except OSError:
        f = ImageFont.load_default()
    cw, ch = 150, 70
    rows = (len(lijst) + kol - 1) // kol
    im = Image.new("RGB", (kol * cw, max(1, rows) * ch), (250, 246, 248))
    d = ImageDraw.Draw(im)
    for i, (b, t, c, h, l, s, var, tx) in enumerate(lijst):
        x, y = (i % kol) * cw, (i // kol) * ch
        im.paste(tx.resize((48, 48), Image.NEAREST), (x + 4, y + 4), tx.resize((48, 48), Image.NEAREST))
        nm = b.replace("minecraft:", "mc:").replace("guhs:", "")
        d.text((x + 56, y + 6), nm[:16], font=f, fill=(60, 30, 50))
        if len(nm) > 16:
            d.text((x + 56, y + 20), nm[16:32], font=f, fill=(60, 30, 50))
        if len(nm) > 32:
            d.text((x + 56, y + 34), nm[32:48], font=f, fill=(60, 30, 50))
    p = os.path.join(OUT, naam + ".png")
    im.save(p)
    print(naam, len(lijst), p)


if __name__ == "__main__":
    data = alle()
    with open(os.path.join(OUT, "blokken.tsv"), "w", encoding="utf-8") as fh:
        for (b, t, c, h, l, s, var, im) in data:
            fh.write("%s\t%s\t%d,%d,%d\t%.0f\t%.2f\t%.2f\t%.3f\t%s\n" % (b, t, *(c * 255), h * 360, l, s, var,
                                                                          groep((b, t, c, h, l, s, var, im)) or ""))
    print("blokken met textuur:", len(data))
    for g in ("roze", "wit_creme", "goud_geel", "hemelblauw"):
        for ns in ("minecraft", "guhs"):
            lst = [d for d in data if groep(d) == g and d[0].startswith(ns)]
            lst.sort(key=lambda d: (d[0].split(":")[1]))
            for k in range(0, len(lst), 240):
                vel(lst[k:k + 240], f"{g}_{ns}_{k // 240}")
    trefw = [d for d in data if any(w in d[0] for w in TREF)]
    trefw.sort(key=lambda d: d[0])
    for k in range(0, len(trefw), 240):
        vel(trefw[k:k + 240], f"trefwoord_{k // 240}")
