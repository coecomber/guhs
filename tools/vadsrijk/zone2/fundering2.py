"""X-Z1GROND: foundations for zone 2 (De Splinterpieken, the zone that opens after zone 1): the same pass and the same
height-based funderingcheck as zone 1 (see tools/vadsrijk/wereld/fundering1.py), with zone 2's own ground heights:
the Kaart2 land height (K.H), the lake's ice / water surface (PL.MEER_IJS) over the lake, the cloud sea (PL.ZEE) elsewhere.

    python tools/vadsrijk/zone2/fundering2.py [--pas]   writes _cache/z2/terrein.npz (and runs the pass on the existing tiles)
maak2 runs grond/paden/schrijf_cache before the tiles and naloop right after them.
"""
import os
import sys

import numpy as np
from scipy import ndimage

HIER = os.path.dirname(os.path.abspath(__file__))
VR = os.path.abspath(os.path.join(HIER, ".."))
sys.path.insert(0, HIER)
sys.path.insert(0, os.path.join(VR, "wereld"))
import plan as PL  # noqa: E402

BEWUST = [(825, -1412, 885, -1383)]   # (world) the quartz/tuff building on stilts on the steep south slope (posts on purpose)


def oorsprong():
    sys.path.insert(0, VR)
    import indeling as IND
    return PL.X0 + IND.Z2_DX, PL.Z0 + IND.Z2_DZ


def grond(K):
    H = K.H.astype(np.int32)
    T = np.where(H >= 0, H, PL.ZEE)
    T = np.where(K.meer & (H >= 0), np.maximum(H, PL.MEER_IJS), T)
    return T.astype(np.int16)


def paden(K):
    return ndimage.binary_dilation(K.pad_m > 0.5, iterations=1)


def schrijf_cache(T, pad):
    x0, z0 = oorsprong()
    d = os.path.join(VR, "_cache", "z2")
    os.makedirs(d, exist_ok=True)
    np.savez_compressed(os.path.join(d, "terrein.npz"), T=T, b=np.array([x0, 0, z0], float), pad=pad)


def naloop(tegels, T, pad, praat=True):
    import fundering1
    x0, z0 = oorsprong()
    return fundering1.naloop(tegels, T, pad, x0, z0, praat=praat, bewust=BEWUST, naam="fundering2")


if __name__ == "__main__":
    import kaart2 as K2
    K_ = K2.Kaart2()
    T_, P_ = grond(K_), paden(K_)
    schrijf_cache(T_, P_)
    print("z2 terrein:", T_.shape, int(T_.min()), int(T_.max()), "oorsprong", oorsprong())
    if "--pas" in sys.argv:
        sys.path.insert(0, VR)
        import indeling as IND
        dtx, dtz = IND.Z2_DX // 64, IND.Z2_DZ // 64
        naloop([(tx + dtx, tz + dtz) for tx in range(PL.UIT_TX0, PL.UIT_TX1 + 1) for tz in range(PL.UIT_TZ0, PL.UIT_TZ1 + 1)], T_, P_)
