"""
U-KRIMP: make the mod jar smaller (user OK 2026-11-06). Re-runnable on the final tree, idempotent.

Three steps over src/main/resources (in this order):
  groot  textures of at least 1024x1024 are halved (2x2 box filter on premultiplied alpha, so no dark fringes). GeckoLib
         normalises UVs by the geo's texture_width/height (not by the image size), and json block/item models use 0..16 UVs,
         so geo/model files stay as they are. Glowmasks/gloed layers have the same size as their base and are halved with it.
  png    lossless recompression of every PNG: exact palette (+tRNS) when <= 256 RGBA colours (1/2/4/8 bit), RGB/L/LA when alpha
         or colour is not needed, zlib 9 (Pillow optimize). The RGBA pixels (incl. the colour of transparent pixels) are checked
         identical before a file is replaced; only written when smaller.
  ogg    Vorbis above ~52 kbps is decoded once and re-encoded to ~48 kbps (libsndfile, like the sound pipeline): mono stays mono,
         stereo becomes mono unless the sides really differ. Same sample rate and sample count (checked). Only written when
         at least 5% smaller. (The Vadsrijk music pipeline itself renders compact now: tools/vadsrijk/geluid/render.py.)

The manifest (tools/krimp_manifest.json, committed) remembers the hash of every file after it was handled: a file with the same
hash is skipped, so lossy steps never run twice on the same file. A file that a generator rewrote (new hash) is handled again.

Run from the project root:
    python tools/krimp.py                  all steps
    python tools/krimp.py --alleen png     only one step (groot | png | ogg)
    python tools/krimp.py --dry            only report what would change
    python tools/krimp.py --rapport        sizes per kind, no changes
Options: --proc N (default 8); --opnieuw redoes the lossless png step for files already in the manifest.
"""
import hashlib
import io
import json
import os
import struct
import sys
from concurrent.futures import ProcessPoolExecutor

ROOT = os.path.join("src", "main", "resources")
MANIFEST = os.path.join("tools", "krimp_manifest.json")
GROOT_MIN = 1024          # both sides >= this -> halved
OGG_MAX_KBPS = 52.0       # above this a file is re-encoded
OGG_DOEL_KBPS = 50.0      # first compression level that lands at or under this wins
OGG_NIVEAUS = (0.85, 0.92, 0.97)   # libsndfile compression_level (Vorbis quality = 1 - level)
OGG_MIN_WINST = 0.05
PNG_SIG = bytes([137, 80, 78, 71, 13, 10, 26, 10])


def sha1(pad):
    with open(pad, "rb") as f:
        return hashlib.sha1(f.read()).hexdigest()


def rel(pad):
    return os.path.relpath(pad, ROOT).replace("\\", "/")


# ---------------------------------------------------------------------------------------------------------------- textures
def halveer(pad, dry=False):
    """Halve a big texture (2x2 box on premultiplied alpha). Returns (old bytes, new bytes) or None."""
    import numpy as np
    from PIL import Image
    with Image.open(pad) as im:
        w, h = im.size
        if w < GROOT_MIN or h < GROOT_MIN or w % 2 or h % 2:
            return None
        a = np.asarray(im.convert("RGBA"), dtype=np.float64) / 255.0
    oud = os.path.getsize(pad)
    if dry:
        return oud, oud // 4
    al = a[..., 3:4]
    pre = a[..., :3] * al
    blok = lambda x: x.reshape(h // 2, 2, w // 2, 2, x.shape[-1]).mean(axis=(1, 3))
    al2, pre2 = blok(al), blok(pre)
    rgb = np.where(al2 > 0, pre2 / np.maximum(al2, 1e-9), blok(a[..., :3]))   # fully transparent: plain mean keeps the colour
    uit = np.concatenate([rgb, al2], axis=-1)
    Image.fromarray(np.clip(np.round(uit * 255.0), 0, 255).astype(np.uint8), "RGBA").save(pad, optimize=True)
    return oud, os.path.getsize(pad)


def _filters(a, bpp):
    """The 5 PNG filters applied to every row of a (h x bytes-per-row uint8)."""
    import numpy as np
    a = a.astype(np.int16)
    links = np.zeros_like(a)
    links[:, bpp:] = a[:, :-bpp]
    boven = np.zeros_like(a)
    boven[1:] = a[:-1]
    lb = np.zeros_like(a)
    lb[1:, bpp:] = a[:-1, :-bpp]
    p = links + boven - lb
    pa, pb, pc = abs(p - links), abs(p - boven), abs(p - lb)
    paeth = np.where((pa <= pb) & (pa <= pc), links, np.where(pb <= pc, boven, lb))
    return [((a - v) & 0xFF).astype(np.uint8) for v in (0, links, boven, (links + boven) >> 1, paeth)]


def _chunk(soort, data):
    import zlib
    return struct.pack(">I", len(data)) + soort + data + struct.pack(">I", zlib.crc32(soort + data) & 0xFFFFFFFF)


def _png_eigen(arr, kleurtype, plte=None, trns=None):
    """Own 8-bit PNG writer that tries every fixed filter + the min-sum heuristic and two zlib strategies (oxipng-light)."""
    import numpy as np
    import zlib
    h, w = arr.shape[:2]
    bpp = 1 if arr.ndim == 2 else arr.shape[2]
    rijen = _filters(arr.reshape(h, w * bpp), bpp)
    keuzes = [np.full(h, t) for t in range(5)]
    som = np.stack([np.abs(r.astype(np.int8).astype(np.int16)).sum(1) for r in rijen])
    keuzes.append(som.argmin(0))
    beste = None
    for k in keuzes:
        raw = np.empty((h, w * bpp + 1), np.uint8)
        raw[:, 0] = k
        for t in range(5):
            m = k == t
            if m.any():
                raw[m, 1:] = rijen[t][m]
        raw = raw.tobytes()
        for strat in (zlib.Z_DEFAULT_STRATEGY, zlib.Z_FILTERED):
            c = zlib.compressobj(9, zlib.DEFLATED, 15, 9, strat)
            z = c.compress(raw) + c.flush()
            if beste is None or len(z) < len(beste):
                beste = z
    uit = PNG_SIG + _chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, kleurtype, 0, 0, 0))
    if plte is not None:
        uit += _chunk(b"PLTE", plte)
    if trns:
        uit += _chunk(b"tRNS", trns)
    return uit + _chunk(b"IDAT", beste) + _chunk(b"IEND", b"")


def _png_kandidaten(rgba):
    """Lossless encodings of an RGBA array; yields PNG bytes (Pillow and the own brute-force writer)."""
    import numpy as np
    from PIL import Image
    h, w, _ = rgba.shape
    opaak = bool((rgba[..., 3] == 255).all())
    grijs = bool(((rgba[..., 0] == rgba[..., 1]) & (rgba[..., 1] == rgba[..., 2])).all())

    def png(im, **kw):
        b = io.BytesIO()
        im.save(b, "PNG", optimize=True, **kw)
        return b.getvalue()

    vormen = [(rgba, "RGBA", 6)]
    if opaak:
        vormen.append((np.ascontiguousarray(rgba[..., :3]), "RGB", 2))
    if grijs:
        if opaak:
            vormen.append((np.ascontiguousarray(rgba[..., 0]), "L", 0))
        else:
            vormen.append((np.ascontiguousarray(rgba[..., [0, 3]]), "LA", 4))
    for arr, modus, kt in vormen:
        yield png(Image.fromarray(arr, modus))
        yield _png_eigen(arr, kt)
    flat = rgba.reshape(-1, 4).view(np.uint32).ravel()
    kleuren, idx = np.unique(flat, return_inverse=True)
    if len(kleuren) <= 256:
        pal = kleuren.view(np.uint8).reshape(-1, 4)
        # sort: transparent entries first, so the tRNS chunk can stop early
        orde = np.argsort(pal[:, 3] == 255, kind="stable")
        terug = np.empty_like(orde)
        terug[orde] = np.arange(len(orde))
        pal = pal[orde]
        idx = terug[idx].astype(np.uint8).reshape(h, w)
        im = Image.fromarray(idx, "P")
        im.putpalette(pal[:, :3].ravel().tobytes(), "RGB")
        n_trans = int((pal[:, 3] < 255).sum())
        kw = {}
        if n_trans:
            kw["transparency"] = pal[:n_trans, 3].tobytes()
        n = len(pal)
        bits = 1 if n <= 2 else 2 if n <= 4 else 4 if n <= 16 else 8
        if bits < 8:
            kw["bits"] = bits
        yield png(im, **kw)
        yield _png_eigen(idx, 3, pal[:, :3].ravel().tobytes(), pal[:n_trans, 3].tobytes() if n_trans else None)


def png_lossless(pad, dry=False):
    """Returns (old bytes, new bytes); new == old when nothing smaller was found."""
    import numpy as np
    from PIL import Image
    with open(pad, "rb") as f:
        data = f.read()
    with Image.open(io.BytesIO(data)) as im:
        if im.mode not in ("RGBA", "RGB", "P", "L", "LA", "1") or getattr(im, "n_frames", 1) > 1:
            return len(data), len(data)
        rgba = np.asarray(im.convert("RGBA"))
    beste = data
    for b in _png_kandidaten(rgba):
        if len(b) < len(beste):
            with Image.open(io.BytesIO(b)) as t:
                if np.array_equal(np.asarray(t.convert("RGBA")), rgba):
                    beste = b
    if beste is not data and not dry:
        with open(pad, "wb") as f:
            f.write(beste)
    return len(data), len(beste)


# ---------------------------------------------------------------------------------------------------------------- ogg
def _vorbis(x, sr, ch, niveau):
    import soundfile as sf
    b = io.BytesIO()
    b.name = "x.ogg"
    with sf.SoundFile(b, "w", sr, ch, format="OGG", subtype="VORBIS", compression_level=niveau) as f:
        stap = max(1, sr // 2)                    # in blocks: one big write overflows libsndfile's stack on Windows
        for a in range(0, len(x), stap):
            f.write(x[a:a + stap])
    return b.getvalue()


def ogg(pad, dry=False):
    """Returns (old bytes, new bytes, note)."""
    import numpy as np
    import soundfile as sf
    oud = os.path.getsize(pad)
    info = sf.info(pad)
    duur = max(info.duration, 1e-3)
    kbps = oud * 8 / 1000 / duur
    if kbps <= OGG_MAX_KBPS:
        return oud, oud, f"{kbps:.0f} kbps, laten"
    x, sr = sf.read(pad, dtype="float32", always_2d=True)
    ch = x.shape[1]
    noot = ""
    if ch == 2:
        mid, zij = (x[:, 0] + x[:, 1]) / 2, (x[:, 0] - x[:, 1]) / 2
        if np.sqrt(np.mean(zij ** 2)) < 0.05 * np.sqrt(np.mean(mid ** 2)) + 1e-9:
            x, ch, noot = mid[:, None], 1, "stereo->mono, "
        else:
            noot = "echt stereo, "
    elif ch > 2:
        return oud, oud, "meer dan 2 kanalen, laten"
    x = np.clip(x, -1.0, 1.0)
    data = x[:, 0] if ch == 1 else x
    beste = None
    for niveau in OGG_NIVEAUS:
        b = _vorbis(data, sr, ch, niveau)
        beste = (b, niveau)
        if len(b) * 8 / 1000 / duur <= OGG_DOEL_KBPS:
            break
    b, niveau = beste
    nieuw = len(b)
    t = sf.info(io.BytesIO(b))
    if t.frames != info.frames or t.samplerate != info.samplerate:
        return oud, oud, f"lengte wijkt af ({t.frames} vs {info.frames}), laten"
    if nieuw > oud * (1 - OGG_MIN_WINST):
        return oud, oud, f"{kbps:.0f} kbps, niet kleiner"
    if not dry:
        with open(pad, "wb") as f:
            f.write(b)
    return oud, nieuw, f"{noot}{kbps:.0f} -> {nieuw * 8 / 1000 / duur:.0f} kbps (niveau {niveau})"


# ---------------------------------------------------------------------------------------------------------------- driver
def werk(item):
    stap, pad, dry = item
    try:
        if stap == "groot":
            r = halveer(pad, dry)
            return stap, pad, (r[0], r[1], "gehalveerd") if r else None
        if stap == "png":
            o, n = png_lossless(pad, dry)
            return stap, pad, (o, n, "")
        o, n, noot = ogg(pad, dry)
        return stap, pad, (o, n, noot)
    except Exception as e:  # noqa: BLE001
        return stap, pad, ("fout", repr(e))


def bestanden(ext):
    for d, _, fs in os.walk(ROOT):
        for f in fs:
            if f.lower().endswith(ext):
                yield os.path.join(d, f)


def rapport():
    tot = {}
    for d, _, fs in os.walk(ROOT):
        for f in fs:
            e = os.path.splitext(f)[1].lower()
            tot[e] = tot.get(e, 0) + os.path.getsize(os.path.join(d, f))
    for e, s in sorted(tot.items(), key=lambda x: -x[1])[:8]:
        print(f"  {e or '(geen)':8s} {s / 1e6:8.2f} MB")


def main(argv):
    dry = "--dry" in argv
    if "--rapport" in argv:
        rapport()
        return
    stappen = ["groot", "png", "ogg"]
    if "--alleen" in argv:
        stappen = [argv[argv.index("--alleen") + 1]]
    opnieuw = "--opnieuw" in argv   # lossless png step only: ignore the manifest (lossy steps never run twice)
    proc = int(argv[argv.index("--proc") + 1]) if "--proc" in argv else 8
    man = {}
    if os.path.exists(MANIFEST):
        man = json.load(open(MANIFEST, encoding="utf-8"))
    for stap in stappen:
        ext = ".ogg" if stap == "ogg" else ".png"
        todo, overgeslagen = [], 0
        for p in bestanden(ext):
            m = man.get(rel(p))
            if m and stap in m.get("stappen", []) and m.get("sha1") == sha1(p) and not (opnieuw and stap == "png"):
                overgeslagen += 1
                continue
            todo.append((stap, p, dry))
        oud = nieuw = 0
        n_veranderd = fouten = 0
        with ProcessPoolExecutor(max_workers=proc) as ex:
            for st, p, r in ex.map(werk, todo, chunksize=4):
                if r is None:
                    continue
                if r[0] == "fout":
                    fouten += 1
                    print(f"  FOUT {rel(p)}: {r[1]}")
                    continue
                o, n, noot = r
                oud += o
                nieuw += n
                if n < o:
                    n_veranderd += 1
                    if st != "png":
                        print(f"  {st}: {rel(p)}  {o / 1024:.0f} -> {n / 1024:.0f} kB  {noot}")
                if not dry:
                    m = man.setdefault(rel(p), {})
                    m["stappen"] = sorted(set(m.get("stappen", [])) | {st})
                    m["sha1"] = sha1(p)
        print(f"{stap}: {len(todo)} bekeken, {overgeslagen} al gedaan (manifest), {n_veranderd} kleiner, {fouten} fouten: "
              f"{oud / 1e6:.2f} -> {nieuw / 1e6:.2f} MB" + (" (dry)" if dry else ""))
        if not dry:
            # a later step rewrites the hash, so earlier steps stay recorded with the newest hash
            with open(MANIFEST, "w", encoding="utf-8", newline="\n") as f:
                json.dump(dict(sorted(man.items())), f, indent=0, sort_keys=True)
                f.write("\n")


if __name__ == "__main__":
    main(sys.argv[1:])
