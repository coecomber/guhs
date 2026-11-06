"""Bouwt de hele wereld (walvis + stad) en bewaart hem in _cache/ (npz), zodat elke opname hem snel laadt.
python wereld.py [palet]   (palet = nieuw | guhs | vanilla; standaard nieuw)"""
import os, sys, time, pickle
import numpy as np
import hoogkit  # noqa  (jars)
import palet
import schemerkit as SK

HERE = os.path.dirname(os.path.abspath(__file__))
CACHE = os.path.join(HERE, "..", "..", "_cache", "z5")   # [zone 5 kaart] tools/vadsrijk/_cache/z5
BOX = (-370, 55, -205, 305, 341, 205)   # [zone 5 kaart] up to local y 340 (= game 320 after the -20 shift)


def log(*a):
    print(time.strftime("%H:%M:%S"), *a, flush=True)


def bouw(modus="nieuw", figuren=True):
    palet.kies(modus)
    import stad
    W = SK.Wereld(*BOX, 1.0)
    S = stad.bouw(W, figuren=figuren, log=log)
    W.stad = None
    return W, S


def bewaar(W, naam):
    os.makedirs(CACHE, exist_ok=True)
    np.save(os.path.join(CACHE, naam + "_v.npy"), W.v)
    meta = dict(pal=W.pal, guhs=W.guhs, extra=W.extra, labels=W.labels, deeltjes=W.deeltjes, b=W.b, k=W.k,
                roof=set())
    for k in ("troon", "uni", "soep", "kaz", "haven", "pieren", "park", "ooggaarde", "balkon_oog"):
        if hasattr(W, k):
            meta[k] = getattr(W, k)
    with open(os.path.join(CACHE, naam + "_meta.pkl"), "wb") as fh:
        pickle.dump(meta, fh)


def laad(naam="nieuw"):
    W = SK.Wereld(*BOX, 1.0) if False else None
    v = np.load(os.path.join(CACHE, naam + "_v.npy"))
    with open(os.path.join(CACHE, naam + "_meta.pkl"), "rb") as fh:
        meta = pickle.load(fh)
    W = SK.Wereld.__new__(SK.Wereld)
    SK.Scene.__init__(W, 1, 1, 1)
    W.v = v
    W.pal = meta["pal"]
    W.idx = {s: i for i, s in enumerate(W.pal)}
    W.guhs, W.extra, W.labels, W.deeltjes = meta["guhs"], meta["extra"], meta["labels"], meta["deeltjes"]
    W.b, W.k = meta["b"], meta["k"]
    W.roof = set()
    X, Y, Z = v.shape
    x1, y1, z1 = W.b[:3]
    W.xs = x1 + (np.arange(X) + 0.5)
    W.ys = y1 + (np.arange(Y) + 0.5)
    W.zs = z1 + (np.arange(Z) + 0.5)
    for k, val in meta.items():
        if k not in ("pal", "guhs", "extra", "labels", "deeltjes", "b", "k", "roof"):
            setattr(W, k, val)
    return W


if __name__ == "__main__":
    modus = sys.argv[1] if len(sys.argv) > 1 else "nieuw"
    t = time.time()
    W, S = bouw(modus)
    bewaar(W, modus)
    log("klaar", W.v.shape, len(W.pal), "blokken", len(W.guhs), "guhs", len(W.extra), "extra quads",
        "%.0fs" % (time.time() - t))
